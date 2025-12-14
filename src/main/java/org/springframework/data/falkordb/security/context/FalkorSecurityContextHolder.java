/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.context;

import org.apiguardian.api.API;

/**
 * Thread-local holder for {@link FalkorSecurityContext}.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public final class FalkorSecurityContextHolder {

	private static final ThreadLocal<FalkorSecurityContext> CONTEXT = new ThreadLocal<>();

	private FalkorSecurityContextHolder() {
	}

	public static FalkorSecurityContext getContext() {
		return CONTEXT.get();
	}

	public static void setContext(FalkorSecurityContext context) {
		CONTEXT.set(context);
	}

	public static void clearContext() {
		CONTEXT.remove();
	}

}
