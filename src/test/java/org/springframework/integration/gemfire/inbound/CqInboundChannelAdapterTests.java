/*
 * Copyright (c) VMware, Inc. 2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.CqEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.gemfire.fork.ForkUtil;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class CqInboundChannelAdapterTests {

	@Autowired
	@Qualifier("test")
	Region<String, Integer> region;

	@Autowired
	ConfigurableApplicationContext applicationContext;

	@Autowired
	PollableChannel outputChannel1;

	@Autowired
	PollableChannel outputChannel2;

	@Autowired
	ContinuousQueryMessageProducer withDurable;

	static OutputStream os;

	@BeforeAll
	public static void startUp() {
		os = ForkUtil.cacheServer();
	}

	@Test
	public void testCqEvent() {
		assertThat(TestUtils.getPropertyValue(withDurable, "durable", Boolean.class)).isTrue();
		region.put("one", 1);
		Message<?> msg = outputChannel1.receive(10000);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload() instanceof CqEvent).isTrue();
	}

	@Test
	public void testPayloadExpression() {
		region.put("one", 1);
		Message<?> msg = outputChannel2.receive(10000);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isEqualTo(1);
	}

	@AfterAll
	public static void cleanUp() {
		sendSignal();
	}

	public static void sendSignal() {
		try {
			os.write("\n".getBytes());
			os.flush();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot communicate with forked VM", ex);
		}
	}

}
