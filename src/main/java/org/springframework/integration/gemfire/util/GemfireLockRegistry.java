/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.util;

import java.util.concurrent.locks.Lock;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;

import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.Assert;

/**
 * Implementation of {@link LockRegistry} providing a distributed lock using Gemfire.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class GemfireLockRegistry implements LockRegistry {

	public static final String LOCK_REGISTRY_REGION = "LockRegistry";

	private final Region<?, ?> region;

	public GemfireLockRegistry(Cache cache) {
		Assert.notNull(cache, "'cache' must not be null");
		this.region = cache.createRegionFactory().setScope(Scope.GLOBAL).create(LOCK_REGISTRY_REGION);
	}

	public GemfireLockRegistry(Region<?, ?> region) {
		Assert.notNull(region, "'region' must not be null");
		this.region = region;
	}

	@Override
	public Lock obtain(Object lockKey) {
		return this.region.getDistributedLock(lockKey);
	}

}
