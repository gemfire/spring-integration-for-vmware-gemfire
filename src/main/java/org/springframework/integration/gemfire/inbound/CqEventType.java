/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.inbound;

/**
 * Enumeration of GemFire Continuous Query Event Types.
 *
 * @author David Turanski
 *
 * @since 2.1
 */
public enum CqEventType {
	CREATED,

	UPDATED,

	DESTROYED,

	REGION_CLEARED,

	REGION_INVALIDATED
}
