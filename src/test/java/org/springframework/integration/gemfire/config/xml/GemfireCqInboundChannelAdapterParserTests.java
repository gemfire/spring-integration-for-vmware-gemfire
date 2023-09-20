/*
 * Copyright (c) VMware, Inc. 2023. All rights reserved.
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
import org.springframework.integration.gemfire.inbound.ContinuousQueryMessageProducer;
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
public class GemfireCqInboundChannelAdapterParserTests {

	private GemfireCqInboundChannelAdapterParser underTest = new GemfireCqInboundChannelAdapterParser();

	@Autowired
	@Qualifier("withDurable")
	ContinuousQueryMessageProducer adapter;

	@Test(expected = BeanDefinitionParsingException.class)
	public void cqListenerContainerIsARequiredAttribute() throws Exception {
		String xml = "<cq-inbound-channel-adapter query=\"some-query\"/>";
		Element element = loadXMLFrom(xml).getDocumentElement();
		underTest.doParse(element, createFakeParserContext(), null);
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void queryIsARequiredAttribute() throws Exception {
		String xml = "<cq-inbound-channel-adapter cq-listener-container=\"some-reference\" />";
		Element element = loadXMLFrom(xml).getDocumentElement();
		underTest.doParse(element, createFakeParserContext(), null);
	}

	@Test
	public void testPhase() {
		assertThat(adapter.getPhase()).isEqualTo(2);
	}

	@Test
	public void testAutoStartup() {
		assertThat(adapter.isAutoStartup()).isEqualTo(false);
	}
}
