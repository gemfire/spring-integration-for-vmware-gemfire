/*
 * Copyright (c) VMware, Inc. 2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author David Turanski
 * @since 2.1
 */
public class GemfireIntegrationNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new GemfireInboundChannelAdapterParser());
		registerBeanDefinitionParser("cq-inbound-channel-adapter", new GemfireCqInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new GemfireOutboundChannelAdapterParser());
	}

}
