/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.outbound;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class CacheWritingMessageHandlerTests {

	private static ClientCacheFactoryBean cacheFactoryBean;

	private static Region<Object, Object> region;

	@BeforeClass
	public static void startUp() throws Exception {
		cacheFactoryBean = new ClientCacheFactoryBean();
		cacheFactoryBean.setBeanFactory(mock(BeanFactory.class));
		cacheFactoryBean.afterPropertiesSet();
		ClientCache cache = (ClientCache) cacheFactoryBean.getObject();
		region = cache.createClientRegionFactory(ClientRegionShortcut.LOCAL).create("sig-tests");
	}

	@AfterClass
	public static void cleanUp() throws Exception {
		if (region != null) {
			region.close();
		}
		if (cacheFactoryBean != null) {
			cacheFactoryBean.destroy();
		}
	}

	@Before
	public void prepare() {
		if (region != null) {
			region.clear();
		}
	}


	@Test
	public void mapPayloadWritesToCache() throws Exception {
		assertThat(region.size()).isEqualTo(0);

		CacheWritingMessageHandler handler = new CacheWritingMessageHandler(region);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Map<String, String> map = new HashMap<String, String>();
		map.put("foo", "bar");
		Message<?> message = MessageBuilder.withPayload(map).build();
		handler.handleMessage(message);
		assertThat(region.size()).isEqualTo(1);
		assertThat(region.get("foo")).isEqualTo("bar");
	}

	@Test
	public void ExpressionsWriteToCache() throws Exception {
		assertThat(region.size()).isEqualTo(0);

		CacheWritingMessageHandler handler = new CacheWritingMessageHandler(region);

		Map<String, String> expressions = new HashMap<String, String>();
		expressions.put("'foo'", "'bar'");
		expressions.put("payload.toUpperCase()", "headers['bar'].toUpperCase()");
		handler.setCacheEntries(expressions);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload("foo")
				.copyHeaders(Collections.singletonMap("bar", "bar"))
				.build();
		handler.handleMessage(message);
		assertThat(region.size()).isEqualTo(2);
		assertThat(region.get("FOO")).isEqualTo("BAR");
		assertThat(region.get("foo")).isEqualTo("bar");

		handler.setCacheEntryExpressions(Collections.<Expression, Expression>singletonMap(new LiteralExpression("baz"),
				new ValueExpression<Long>(10L)));

		handler.handleMessage(new GenericMessage<String>("test"));
		assertThat(region.size()).isEqualTo(3);
		assertThat(region.get("baz")).isEqualTo(10L);
	}

}
