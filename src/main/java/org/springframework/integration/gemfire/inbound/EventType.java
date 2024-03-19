/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.inbound;

/**
 * Enumeration of GemFire event types.
 *
 * @author Mark Fisher
 * @since 2.1
 */
public enum EventType {

	CREATED,

	UPDATED,

	DESTROYED,

	INVALIDATED

}
