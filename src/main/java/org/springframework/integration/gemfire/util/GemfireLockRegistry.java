/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.util;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link LockRegistry} providing a distributed lock using Gemfire.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class GemfireLockRegistry implements LockRegistry {

  public static final String LOCK_REGISTRY_REGION = "LockRegistry";

  private final Region region;

  public GemfireLockRegistry(ClientCache cache) {
    Assert.notNull(cache, "'cache' must not be null");
    this.region = cache.createClientRegionFactory(ClientRegionShortcut.LOCAL).create(LOCK_REGISTRY_REGION);
  }

  public GemfireLockRegistry(Region<Object, Object> region) {
    Assert.notNull(region, "'region' must not be null");
    this.region = region;
  }

  @Override
  public synchronized Lock obtain(Object lockKey) {
    Lock value = (Lock) region.get(lockKey);
    if (value == null) {
      value = new ReentrantLock();
      this.region.put(lockKey, value);
    }
    return value;
  }
}
