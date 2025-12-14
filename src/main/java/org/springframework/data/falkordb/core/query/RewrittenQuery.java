/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.core.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Simple value type holding a rewritten query and its (possibly updated) parameter map.
 */
public final class RewrittenQuery {

	private final String cypher;
	private final Map<String, Object> parameters;

	public RewrittenQuery(String cypher, Map<String, Object> parameters) {
		Assert.hasText(cypher, "Cypher query must not be null or empty");
		Assert.notNull(parameters, "Parameters must not be null");
		this.cypher = cypher;
		this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
	}

	public static RewrittenQuery of(String cypher, Map<String, Object> parameters) {
		return new RewrittenQuery(cypher, parameters);
	}

	public String getCypher() {
		return this.cypher;
	}

	public Map<String, Object> getParameters() {
		return this.parameters;
	}

}