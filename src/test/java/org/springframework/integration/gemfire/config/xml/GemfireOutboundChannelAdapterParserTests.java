/*
 * Copyright (c) VMware, Inc. 2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.createFakeParserContext;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.loadXMLFrom;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Oxlade
 * @author Liujiong
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class GemfireOutboundChannelAdapterParserTests {

	private final GemfireOutboundChannelAdapterParser underTest = new GemfireOutboundChannelAdapterParser();

	private static final CountDownLatch adviceCalled = new CountDownLatch(1);

	@Autowired
	@Qualifier("adapter")
	ConsumerEndpointFactoryBean adapter1;

	@Autowired
	ApplicationContext ctx;

	@Test(expected = BeanDefinitionParsingException.class)
	public void regionIsARequiredAttribute() throws Exception {
		String xml = "<outbound-channel-adapter />";
		Element element = loadXMLFrom(xml).getDocumentElement();
		underTest.parseConsumer(element, createFakeParserContext());
	}

	@Test
	public void withAdvice() throws InterruptedException {
		adapter1.start();
		MessageChannel channel = ctx.getBean("input", MessageChannel.class);
		channel.send(new GenericMessage<String>("foo"));
		assertThat(adviceCalled.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testPhase() {
		assertThat(adapter1.getPhase()).isEqualTo(2);
	}

	@Test
	public void testAutoStart() {
		assertThat(adapter1.isAutoStartup()).isEqualTo(false);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled.countDown();
			return null;
		}

	}
}
