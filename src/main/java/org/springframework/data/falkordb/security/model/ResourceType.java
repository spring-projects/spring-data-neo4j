/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.model;

import org.apiguardian.api.API;

/**
 * Resource type for a privilege.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public enum ResourceType {

	NODE,

	PROPERTY,

	RELATIONSHIP

}
