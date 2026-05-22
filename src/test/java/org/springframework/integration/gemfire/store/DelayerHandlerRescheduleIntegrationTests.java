/*
 * Copyright 2023-2026 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.store;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
@LongRunningTest
public class DelayerHandlerRescheduleIntegrationTests {

	public static final String DELAYER_ID = "delayerWithGemfireMS";

	public static Region<Object, Object> region;

	private static ClientCacheFactoryBean cacheFactoryBean;

	@BeforeAll
	public static void startUp() throws Exception {
		cacheFactoryBean = new ClientCacheFactoryBean();
		cacheFactoryBean.afterPropertiesSet();
		Cache cache = (Cache) cacheFactoryBean.getObject();
		region = cache.createRegionFactory().setScope(Scope.LOCAL).create("sig-tests");
	}

	@AfterAll
	public static void cleanUp() throws Exception {
		if (region != null) {
			region.close();
		}
		if (cacheFactoryBean != null) {
			cacheFactoryBean.destroy();
		}
	}

	@Test
	public void testDelayerHandlerRescheduleWithGemfireMessageStore() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"DelayerHandlerRescheduleIntegrationTests-context.xml", this.getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);

		String delayerMessageGroupId = DELAYER_ID + ".messageGroupId";

		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		input.send(message1);
		input.send(MessageBuilder.withPayload("test2").build());

		// Emulate restart and check Cache state before next start
		// Interrupt taskScheduler as quickly as possible
		ThreadPoolTaskScheduler taskScheduler =
				(ThreadPoolTaskScheduler) IntegrationContextUtils.getTaskScheduler(context);
		taskScheduler.shutdown();
		taskScheduler.getScheduledExecutor().awaitTermination(10, TimeUnit.SECONDS);
		context.close();

		try {
			context.getBean("input", MessageChannel.class);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e instanceof IllegalStateException).isTrue();
			assertThat(e.getMessage().contains("BeanFactory not initialized or already closed - call 'refresh'"))
					.isTrue();
		}

		assertThat(messageStore.getMessageGroupCount()).isEqualTo(1);
		assertThat(messageStore.iterator().next().getGroupId()).isEqualTo(delayerMessageGroupId);
		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(2);
		assertThat(messageStore.getMessageCountForAllMessageGroups()).isEqualTo(2);
		MessageGroup messageGroup = messageStore.getMessageGroup(delayerMessageGroupId);
		Message<?> messageInStore = messageGroup.getMessages().iterator().next();
		Object payload = messageInStore.getPayload();

		// INT-3049
		assertThat(payload instanceof DelayHandler.DelayedMessageWrapper).isTrue();
		assertThat(((DelayHandler.DelayedMessageWrapper) payload).getOriginal()).isEqualTo(message1);

		context.refresh();

		PollableChannel output = context.getBean("output", PollableChannel.class);

		Message<?> message = output.receive(20000);
		assertThat(message).isNotNull();

		Object payload1 = message.getPayload();

		message = output.receive(20000);
		assertThat(message).isNotNull();
		Object payload2 = message.getPayload();
		assertThat(payload2).isNotSameAs(payload1);

		assertThat(messageStore.getMessageGroupCount()).isEqualTo(1);
		int n = 0;
		while (n++ < 200 && messageStore.messageGroupSize(delayerMessageGroupId) > 0) {
			Thread.sleep(100);
		}
		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(0);

		context.close();
	}

}
