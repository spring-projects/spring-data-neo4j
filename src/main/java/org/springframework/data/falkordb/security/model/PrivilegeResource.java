/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.model;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Utility for converting typed privilege resources into canonical resource strings.
 */
public final class PrivilegeResource {

	private PrivilegeResource() {
	}

	/**
	 * Canonical string representation used by {@code FalkorSecurityContext.can(action, resource)}.
	 */
	public static String toResourceString(ResourceType type, String resourceLabel, @Nullable String resourceProperty) {
		if (type == null) {
			return resourceLabel;
		}
		if (!StringUtils.hasText(resourceLabel)) {
			return null;
		}
		return switch (type) {
			case NODE, RELATIONSHIP -> resourceLabel;
			case PROPERTY -> StringUtils.hasText(resourceProperty) ? resourceLabel + "." + resourceProperty : resourceLabel;
		};
	}

	@Nullable
	public static String toResourceString(Privilege privilege) {
		if (privilege == null) {
			return null;
		}
		if (StringUtils.hasText(privilege.getResource())) {
			return privilege.getResource();
		}
		return toResourceString(privilege.getResourceType(), privilege.getResourceLabel(), privilege.getResourceProperty());
	}
}
