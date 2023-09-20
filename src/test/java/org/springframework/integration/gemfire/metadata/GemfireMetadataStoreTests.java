/*
 * Copyright (c) VMware, Inc. 2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

/**
 * @author Artem Bilan
 * @since 4.0
 *
 */
public class GemfireMetadataStoreTests {

	private static Cache cache;

	private static ConcurrentMetadataStore metadataStore;

	private static Region<Object, Object> region;

	@BeforeClass
	public static void startUp() throws Exception {
		cache = new CacheFactory().create();
		metadataStore = new GemfireMetadataStore(cache);
		region = cache.getRegion(GemfireMetadataStore.KEY);
	}

	@AfterClass
	public static void cleanUp() {
		if (region != null) {
			region.close();
		}
		if (cache != null) {
			cache.close();
			assertThat(cache.isClosed()).as("Cache did not close after close() call").isTrue();
		}
	}

	@Before
	@After
	public void setup() {
		if (region != null) {
			region.clear();
		}
	}

	@Test
	public void testGetNonExistingKeyValue() {
		String retrievedValue = metadataStore.get("does-not-exist");
		assertThat(retrievedValue).isNull();
	}

	@Test
	public void testPersistKeyValue() {
		metadataStore.put("GemfireMetadataStoreTests-Spring", "Integration");

		GemfireTemplate gemfireTemplate = new GemfireTemplate(region);

		Object v = gemfireTemplate.get("GemfireMetadataStoreTests-Spring");
		assertThat(v).isEqualTo("Integration");
	}

	@Test
	public void testGetValueFromMetadataStore() {
		metadataStore.put("GemfireMetadataStoreTests-GetValue", "Hello Gemfire");

		String retrievedValue = metadataStore.get("GemfireMetadataStoreTests-GetValue");
		assertThat(retrievedValue).isEqualTo("Hello Gemfire");
	}

	@Test
	public void testPersistEmptyStringToMetadataStore() {
		metadataStore.put("GemfireMetadataStoreTests-PersistEmpty", "");

		String retrievedValue = metadataStore.get("GemfireMetadataStoreTests-PersistEmpty");
		assertThat(retrievedValue).isEqualTo("");
	}

	@Test
	public void testPersistNullStringToMetadataStore() {
		try {
			metadataStore.put("GemfireMetadataStoreTests-PersistEmpty", null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'value' must not be null.");
		}
	}

	@Test
	public void testPersistWithEmptyKeyToMetadataStore() {
		metadataStore.put("", "PersistWithEmptyKey");

		String retrievedValue = metadataStore.get("");
		assertThat(retrievedValue).isEqualTo("PersistWithEmptyKey");
	}

	@Test
	public void testPersistWithNullKeyToMetadataStore() {
		try {
			metadataStore.put(null, "something");
			fail("Expected an IllegalArgumentException to be thrown.");

		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'key' must not be null.");
		}
	}

	@Test
	public void testGetValueWithNullKeyFromMetadataStore() {
		try {
			metadataStore.get(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'key' must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testRemoveFromMetadataStore() {
		String testKey = "GemfireMetadataStoreTests-Remove";
		String testValue = "Integration";

		metadataStore.put(testKey, testValue);

		assertThat(metadataStore.remove(testKey)).isEqualTo(testValue);
		assertThat(metadataStore.remove(testKey)).isNull();
	}

	@Test
	public void testPersistKeyValueIfAbsent() {
		metadataStore.putIfAbsent("GemfireMetadataStoreTests-Spring", "Integration");

		GemfireTemplate gemfireTemplate = new GemfireTemplate(region);

		Object v = gemfireTemplate.get("GemfireMetadataStoreTests-Spring");
		assertThat(v).isEqualTo("Integration");
	}

}
