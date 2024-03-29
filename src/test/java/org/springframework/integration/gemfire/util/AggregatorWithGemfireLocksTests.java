/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AggregatorWithGemfireLocksTests {

	@Autowired
	private LatchingReleaseStrategy releaseStrategy;

	@Autowired
	private MessageChannel in;

	@Autowired
	private MessageChannel in2;

	@Autowired
	private PollableChannel out;

	private volatile Exception exception;

	@Test
	public void testLockSingleGroup() throws Exception {
		this.releaseStrategy.reset(1);
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 1));
		Executors.newSingleThreadExecutor().execute(asyncSend("bar", 2, 1));
		assertThat(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS)).isTrue();
		this.releaseStrategy.latch1.countDown();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.releaseStrategy.maxCallers.get()).isEqualTo(1);
		assertThat(this.exception)
				.as("Unexpected exception:" + (this.exception != null ? this.exception.toString() : "")).isNull();
	}

	@Test
	public void testLockThreeGroups() throws Exception {
		this.releaseStrategy.reset(3);
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 1));
		Executors.newSingleThreadExecutor().execute(asyncSend("bar", 2, 1));
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 2));
		Executors.newSingleThreadExecutor().execute(asyncSend("bar", 2, 2));
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 3));
		Executors.newSingleThreadExecutor().execute(asyncSend("bar", 2, 3));
		assertThat(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS)).isTrue();
		this.releaseStrategy.latch1.countDown();
		this.releaseStrategy.latch1.countDown();
		this.releaseStrategy.latch1.countDown();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.releaseStrategy.maxCallers.get()).isEqualTo(3);
		assertThat(this.exception)
				.as("Unexpected exception:" + (this.exception != null ? this.exception.toString() : "")).isNull();
	}

	@Test
	public void testDistributedAggregator() throws Exception {
		this.releaseStrategy.reset(1);
		Executors.newSingleThreadExecutor().execute(asyncSend("foo", 1, 1));
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				in2.send(new GenericMessage<String>("bar", stubHeaders(2, 2, 1)));
			}
			catch (Exception e) {
				exception = e;
			}
		});
		assertThat(this.releaseStrategy.latch2.await(10, TimeUnit.SECONDS)).isTrue();
		this.releaseStrategy.latch1.countDown();
		assertThat(this.out.receive(10000)).isNotNull();
		assertThat(this.releaseStrategy.maxCallers.get()).isEqualTo(1);
		assertThat(this.exception)
				.as("Unexpected exception:" + (this.exception != null ? this.exception.toString() : "")).isNull();
	}

	private Runnable asyncSend(final String payload, final int sequence, final int correlation) {
		return () -> {
			try {
				in.send(new GenericMessage<String>(payload, stubHeaders(sequence, 2, correlation)));
			}
			catch (Exception e) {
				exception = e;
			}
		};
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correlationId) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId);
		return headers;
	}

	public static class LatchingReleaseStrategy implements ReleaseStrategy {

		private volatile CountDownLatch latch1;

		private volatile CountDownLatch latch2;

		private volatile AtomicInteger callers;

		private volatile AtomicInteger maxCallers;

		@Override
		public boolean canRelease(MessageGroup group) {
			synchronized (this) {
				this.callers.incrementAndGet();
				this.maxCallers.set(Math.max(this.maxCallers.get(), this.callers.get()));
			}
			this.latch2.countDown();
			try {
				this.latch1.await(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			this.callers.decrementAndGet();
			return group.size() > 1;
		}

		public void reset(int expectedConcurrency) {
			this.latch1 = new CountDownLatch(expectedConcurrency);
			this.latch2 = new CountDownLatch(expectedConcurrency);
			this.callers = new AtomicInteger();
			this.maxCallers = new AtomicInteger();
		}

	}

}
