/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.policy;

import org.springframework.data.falkordb.security.model.ResourceType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Parser for resource patterns.
 *
 * Milestone-2 choice: use '#' as the property separator to avoid ambiguity with
 * fully-qualified Java class names (which contain '.').
 *
 * Examples:
 * - NODE: com.acme.Foo
 * - PROPERTY: com.acme.Foo#bar
 * - RELATIONSHIP: KNOWS
 */
public record ResourcePattern(ResourceType type, String labelOrType, @Nullable String property) {

	public static ResourcePattern parse(String pattern) {
		Assert.hasText(pattern, "pattern must not be empty");

		String trimmed = pattern.trim();

		int hash = trimmed.indexOf('#');
		if (hash > 0 && hash < trimmed.length() - 1) {
			String label = trimmed.substring(0, hash);
			String prop = trimmed.substring(hash + 1);
			return new ResourcePattern(ResourceType.PROPERTY, label, prop);
		}

		if (trimmed.matches("^[A-Z0-9_]+$")) {
			return new ResourcePattern(ResourceType.RELATIONSHIP, trimmed, null);
		}

		return new ResourcePattern(ResourceType.NODE, trimmed, null);
	}
}
