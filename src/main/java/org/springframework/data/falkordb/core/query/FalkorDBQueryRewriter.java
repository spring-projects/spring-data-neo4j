/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.core.query;

import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Hook to allow rewriting or augmenting Cypher queries prior to execution.
 *
 * Intended use: row-level security / policy enforcement.
 */
@FunctionalInterface
public interface FalkorDBQueryRewriter {

	RewrittenQuery rewrite(String cypher, Map<String, Object> parameters, @Nullable Class<?> domainType);

}