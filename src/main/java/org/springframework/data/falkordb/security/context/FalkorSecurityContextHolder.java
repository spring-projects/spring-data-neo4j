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

	@FunctionalInterface
	public interface Scope extends AutoCloseable {
		@Override
		void close();
	}

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

	/**
	 * Set the given context for the current thread and return a scope that restores
	 * the previous context when closed.
	 */
	public static Scope withContext(FalkorSecurityContext context) {
		final FalkorSecurityContext previous = getContext();
		setContext(context);
		return () -> {
			if (previous == null) {
				clearContext();
			}
			else {
				setContext(previous);
			}
		};
	}

}
