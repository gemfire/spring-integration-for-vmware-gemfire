/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.gemfire.inbound.CacheListeningMessageProducer;

/**
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class GemfireInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	private static final String ERROR_CHANNEL_ATTRIBUTE = "error-channel";

	private static final String OUTPUT_CHANNEL_PROPERTY = "outputChannel";

	private static final String REGION_ATTRIBUTE = "region";

	private static final String EXPRESSION_ATTRIBUTE = "expression";

	private static final String SUPPORTED_EVENT_TYPES_PROPERTY = "supportedEventTypes";

	private static final String CACHE_EVENTS_ATTRIBUTE = "cache-events";

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder listeningMessageProducer =
				BeanDefinitionBuilder.genericBeanDefinition(CacheListeningMessageProducer.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(listeningMessageProducer, element,
				EXPRESSION_ATTRIBUTE, "payloadExpressionString");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(listeningMessageProducer, element,
				CACHE_EVENTS_ATTRIBUTE, SUPPORTED_EVENT_TYPES_PROPERTY);

		if (!element.hasAttribute(REGION_ATTRIBUTE)) {
			parserContext.getReaderContext().error("'region' attribute is required.", element);
		}

		listeningMessageProducer.addConstructorArgReference(element.getAttribute(REGION_ATTRIBUTE));

		listeningMessageProducer.addPropertyReference(OUTPUT_CHANNEL_PROPERTY, channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(listeningMessageProducer, element,
				ERROR_CHANNEL_ATTRIBUTE);
		return listeningMessageProducer.getBeanDefinition();
	}

}
