/*
 * Copyright (c) VMware, Inc. 2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.gemfire.inbound.ContinuousQueryMessageProducer;

/**
 * @author David Turanski
 * @author Dan Oxlade
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 *
 */
public class GemfireCqInboundChannelAdapterParser extends AbstractChannelAdapterParser {


	private static final String ERROR_CHANNEL_ATTRIBUTE = "error-channel";

	private static final String OUTPUT_CHANNEL_PROPERTY = "outputChannel";

	private static final String QUERY_LISTENER_CONTAINER_ATTRIBUTE = "cq-listener-container";

	private static final String DURABLE_ATTRIBUTE = "durable";

	private static final String QUERY_NAME_ATTRIBUTE = "query-name";

	private static final String QUERY_ATTRIBUTE = "query";

	private static final String EXPRESSION_ATTRIBUTE = "expression";

	private static final String SUPPORTED_EVENT_TYPES_PROPERTY = "supportedEventTypes";

	private static final String QUERY_EVENTS_ATTRIBUTE = "query-events";

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder continuousQueryMessageProducer =
				BeanDefinitionBuilder.genericBeanDefinition(ContinuousQueryMessageProducer.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(continuousQueryMessageProducer, element,
				EXPRESSION_ATTRIBUTE, "payloadExpressionString");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(continuousQueryMessageProducer, element,
				QUERY_EVENTS_ATTRIBUTE, SUPPORTED_EVENT_TYPES_PROPERTY);

		if (!element.hasAttribute(QUERY_LISTENER_CONTAINER_ATTRIBUTE)) {
			parserContext.getReaderContext()
					.error("'" + QUERY_LISTENER_CONTAINER_ATTRIBUTE + "' attribute is required.", element);
		}

		if (!element.hasAttribute(QUERY_ATTRIBUTE)) {
			parserContext.getReaderContext().error("'" + QUERY_ATTRIBUTE + "' attribute is required.", element);
		}

		continuousQueryMessageProducer.addConstructorArgReference(element.getAttribute(QUERY_LISTENER_CONTAINER_ATTRIBUTE));
		continuousQueryMessageProducer.addConstructorArgValue(element.getAttribute(QUERY_ATTRIBUTE));

		continuousQueryMessageProducer.addPropertyReference(OUTPUT_CHANNEL_PROPERTY, channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(continuousQueryMessageProducer, element,
				ERROR_CHANNEL_ATTRIBUTE);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(continuousQueryMessageProducer, element, QUERY_NAME_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(continuousQueryMessageProducer, element, DURABLE_ATTRIBUTE);
		return continuousQueryMessageProducer.getBeanDefinition();
	}

}
