/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.annotation;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.security.model.Action;

/**
 * Utility for resolving {@link Secured} and {@link RowLevelSecurity} annotations
 * into {@link SecurityMetadata}.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public final class SecurityMetadataUtils {

	private SecurityMetadataUtils() {
	}

	public static SecurityMetadata resolveMetadata(Class<?> entityType) {
		Secured secured = entityType.getAnnotation(Secured.class);
		RowLevelSecurity rls = entityType.getAnnotation(RowLevelSecurity.class);

		Map<Action, Set<String>> rolesByAction = new EnumMap<>(Action.class);
		Map<Action, Map<String, Set<String>>> deniedByAction = new EnumMap<>(Action.class);

		if (secured != null) {
			putRoles(rolesByAction, Action.READ, secured.read());
			putRoles(rolesByAction, Action.WRITE, secured.write());
			putRoles(rolesByAction, Action.CREATE, secured.create());
			putRoles(rolesByAction, Action.DELETE, secured.delete());

			putDenied(deniedByAction, Action.READ, secured.denyReadProperties());
			putDenied(deniedByAction, Action.WRITE, secured.denyWriteProperties());
		}

		String filter = rls != null ? rls.filter() : null;

		return new SecurityMetadata(entityType, rolesByAction, deniedByAction, filter);
	}

	private static void putRoles(Map<Action, Set<String>> rolesByAction, Action action, String[] roles) {
		if (roles == null || roles.length == 0) {
			return;
		}
		Set<String> set = rolesByAction.computeIfAbsent(action, k -> new HashSet<>());
		set.addAll(Arrays.asList(roles));
	}

	private static void putDenied(Map<Action, Map<String, Set<String>>> deniedByAction, Action action,
			DenyProperty[] denyProperties) {
		if (denyProperties == null || denyProperties.length == 0) {
			return;
		}
		Map<String, Set<String>> perProperty = deniedByAction.computeIfAbsent(action, k -> new HashMap<>());
		for (DenyProperty deny : denyProperties) {
			if (deny.property() == null || deny.property().isEmpty()) {
				continue;
			}
			Set<String> roles = perProperty.computeIfAbsent(deny.property(), k -> new HashSet<>());
			roles.addAll(Arrays.asList(deny.forRoles()));
		}
	}

}
