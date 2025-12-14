/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.annotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.security.model.Action;

/**
 * Resolved security metadata for a given entity type.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public final class SecurityMetadata {

	private final Class<?> entityType;

	private final Map<Action, Set<String>> rolesByAction;

	private final Map<Action, Map<String, Set<String>>> deniedPropertiesByAction;

	private final String rowFilterExpression;

	SecurityMetadata(Class<?> entityType, Map<Action, Set<String>> rolesByAction,
			Map<Action, Map<String, Set<String>>> deniedPropertiesByAction, String rowFilterExpression) {
		this.entityType = entityType;
		this.rolesByAction = copyRolesByAction(rolesByAction);
		this.deniedPropertiesByAction = copyDeniedByAction(deniedPropertiesByAction);
		this.rowFilterExpression = rowFilterExpression;
	}

	public Class<?> getEntityType() {
		return this.entityType;
	}

	public Set<String> getRolesFor(Action action) {
		Set<String> roles = this.rolesByAction.get(action);
		return roles != null ? roles : Collections.emptySet();
	}

	public Map<String, Set<String>> getDeniedPropertiesFor(Action action) {
		Map<String, Set<String>> map = this.deniedPropertiesByAction.get(action);
		return map != null ? map : Collections.emptyMap();
	}

	public String getRowFilterExpression() {
		return this.rowFilterExpression;
	}

	private Map<Action, Set<String>> copyRolesByAction(Map<Action, Set<String>> source) {
		Map<Action, Set<String>> copy = new HashMap<>();
		if (source != null) {
			for (Map.Entry<Action, Set<String>> entry : source.entrySet()) {
				copy.put(entry.getKey(), Collections.unmodifiableSet(new HashSet<>(entry.getValue())));
			}
		}
		return Collections.unmodifiableMap(copy);
	}

	private Map<Action, Map<String, Set<String>>> copyDeniedByAction(
			Map<Action, Map<String, Set<String>>> source) {
		Map<Action, Map<String, Set<String>>> copy = new HashMap<>();
		if (source != null) {
			for (Map.Entry<Action, Map<String, Set<String>>> entry : source.entrySet()) {
				Map<String, Set<String>> perProperty = new HashMap<>();
				for (Map.Entry<String, Set<String>> p : entry.getValue().entrySet()) {
					perProperty.put(p.getKey(), Collections.unmodifiableSet(new HashSet<>(p.getValue())));
				}
				copy.put(entry.getKey(), Collections.unmodifiableMap(perProperty));
			}
		}
		return Collections.unmodifiableMap(copy);
	}

}
