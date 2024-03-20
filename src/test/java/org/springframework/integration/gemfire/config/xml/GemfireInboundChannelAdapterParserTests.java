/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.createFakeParserContext;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.loadXMLFrom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.integration.gemfire.inbound.CacheListeningMessageProducer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Oxlade
 * @author Liujiong
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class GemfireInboundChannelAdapterParserTests {

	private GemfireInboundChannelAdapterParser underTest = new GemfireInboundChannelAdapterParser();

	@Autowired
	@Qualifier("channel1.adapter")
	CacheListeningMessageProducer adapter1;

	@Test(expected = BeanDefinitionParsingException.class)
	public void regionIsARequiredAttribute() throws Exception {
		String xml = "<inbound-channel-adapter />";
		Element element = loadXMLFrom(xml).getDocumentElement();
		underTest.doParse(element, createFakeParserContext(), null);
	}

	@Test
	public void testPhase() {
		assertThat(adapter1.getPhase()).isEqualTo(2);
	}

	@Test
	public void testAutoStart() {
		assertThat(adapter1.isAutoStartup()).isEqualTo(false);
	}

}
