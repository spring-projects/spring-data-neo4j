/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.model;

import org.apiguardian.api.API;

/**
 * Supported security actions for RBAC checks.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public enum Action {

	READ,

	WRITE,

	CREATE,

	DELETE

}
