/*
 * Copyright (c) VMware, Inc. 2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.geode.cache.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */

@SpringJUnitConfig
@DirtiesContext
public class GemfireOutboundChannelAdapterTests {

	@Autowired
	MessageChannel cacheChannel1;

	@Autowired
	@Qualifier("region1")
	Region<String, String> region1;

	@Autowired
	MessageChannel cacheChannel2;

	@Autowired
	@Qualifier("region2")
	Region<String, String> region2;

	@Autowired
	MessageChannel cacheChainChannel;


	@BeforeEach
	public void setUp() {
		region1.clear();
		region2.clear();
	}

	@Test
	public void testWriteMapPayload() {
		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");

		Message<?> message = MessageBuilder.withPayload(map).build();
		cacheChannel1.send(message);
		assertThat(region1.size()).isEqualTo(1);
		assertThat(region1.get("foo")).isEqualTo("bar");
	}

	@Test
	public void testWriteExpressions() {
		Message<?> message = MessageBuilder.withPayload("Hello").build();
		cacheChannel2.send(message);
		assertThat(region2.size()).isEqualTo(2);
		assertThat(region2.get("HELLO")).isEqualTo("hello");
		assertThat(region2.get("foo")).isEqualTo("bar");
	}

	@Test
	public void testWriteWithinChain() {
		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");

		Message<?> message = MessageBuilder.withPayload(map).build();
		cacheChainChannel.send(message);
		assertThat(region1.size()).isEqualTo(1);
		assertThat(region1.get("foo")).isEqualTo("bar");
	}

}
