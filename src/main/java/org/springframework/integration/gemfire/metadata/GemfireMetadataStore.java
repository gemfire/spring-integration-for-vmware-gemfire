/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.metadata;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.util.CacheListenerAdapter;

import org.springframework.integration.metadata.ListenableMetadataStore;
import org.springframework.integration.metadata.MetadataStoreListener;
import org.springframework.util.Assert;

/**
 * Gemfire implementation of {@link ListenableMetadataStore}.
 * Use this {@link org.springframework.integration.metadata.MetadataStore}
 * to achieve meta-data persistence shared across application instances and
 * restarts.
 *
 * @author Artem Bilan
 * @author Venil Noronha
 * @author Gary Russell
 *
 * @since 4.0
 */
public class GemfireMetadataStore implements ListenableMetadataStore {

	private static final String KEY_MUST_NOT_BE_NULL = "'key' must not be null.";

	public static final String KEY = "MetaData";

	private final GemfireCacheListener cacheListener = new GemfireCacheListener();

	private final Region<String, String> region;

	public GemfireMetadataStore(Cache cache) {
		this(Objects.requireNonNull(cache, "'cache' must not be null")
				.<String, String>createRegionFactory()
				.setScope(Scope.LOCAL)
				.create(KEY));
	}

	public GemfireMetadataStore(Region<String, String> region) {
		Assert.notNull(region, "'region' must not be null");
		this.region = region;
		this.region.getAttributesMutator()
				.addCacheListener(this.cacheListener);
	}

	@Override
	public void put(String key, String value) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		Assert.notNull(value, "'value' must not be null.");
		this.region.put(key, value);
	}

	@Override
	public String putIfAbsent(String key, String value) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		Assert.notNull(value, "'value' must not be null.");
		return this.region.putIfAbsent(key, value);
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		Assert.notNull(oldValue, "'oldValue' must not be null.");
		Assert.notNull(newValue, "'newValue' must not be null.");
		return this.region.replace(key, oldValue, newValue);
	}

	@Override
	public String get(String key) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		return this.region.get(key);
	}

	@Override
	public String remove(String key) {
		Assert.notNull(key, KEY_MUST_NOT_BE_NULL);
		return this.region.remove(key);
	}

	@Override
	public void addListener(MetadataStoreListener listener) {
		Assert.notNull(listener, "'listener' must not be null");
		this.cacheListener.listeners.add(listener);
	}

	@Override
	public void removeListener(MetadataStoreListener listener) {
		this.cacheListener.listeners.remove(listener);
	}

	private static class GemfireCacheListener extends CacheListenerAdapter<String, String> {

		private final List<MetadataStoreListener> listeners = new CopyOnWriteArrayList<>();

		GemfireCacheListener() {
		}

		@Override
		public void afterCreate(EntryEvent<String, String> event) {
			this.listeners.forEach(listener -> listener.onAdd(event.getKey(), event.getNewValue()));
		}

		@Override
		public void afterUpdate(EntryEvent<String, String> event) {
			this.listeners.forEach(listener -> listener.onUpdate(event.getKey(), event.getNewValue()));
		}

		@Override
		public void afterDestroy(EntryEvent<String, String> event) {
			this.listeners.forEach(listener -> listener.onRemove(event.getKey(), event.getOldValue()));
		}

	}

}
