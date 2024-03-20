/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.config.xml;

import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.InputStreamResource;

/**
 * @author Dan Oxlade
 * @author Gary Russell
 */
class ParserTestUtil {

	private ParserTestUtil() {
		super();
	}

	static ParserContext createFakeParserContext() {
		return new ParserContext(
				new XmlReaderContext(thisClassAsResource(), new FailFastProblemReporter(), null, null, null, null),
				null);
	}

	static InputStreamResource thisClassAsResource() {
		return new InputStreamResource(
				ParserTestUtil.class.getResourceAsStream(ParserTestUtil.class.getSimpleName() + ".class"));
	}

	static org.w3c.dom.Document loadXMLFrom(String xml) throws org.xml.sax.SAXException, java.io.IOException {
		return loadXMLFrom(new java.io.ByteArrayInputStream(xml.getBytes()));
	}

	static org.w3c.dom.Document loadXMLFrom(java.io.InputStream is)
			throws org.xml.sax.SAXException, java.io.IOException {
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		javax.xml.parsers.DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		}
		catch (javax.xml.parsers.ParserConfigurationException ex) {
		}
		org.w3c.dom.Document doc = builder.parse(is);
		is.close();
		return doc;
	}

}
