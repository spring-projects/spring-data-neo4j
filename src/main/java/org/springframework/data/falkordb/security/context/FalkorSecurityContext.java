/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.context;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.security.model.Action;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Falkor-specific security context used for RBAC checks.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class FalkorSecurityContext {

	/**
	 * Default role name included for all users unless explicitly disabled.
	 */
	public static final String DEFAULT_DEFAULT_ROLE = "PUBLIC";

	private final User user;

	private final Set<String> effectiveRoles;

	private final Set<Privilege> privileges;

	public FalkorSecurityContext(User user, Set<Role> roles, Set<Privilege> privileges) {
		this(user, roles, privileges, DEFAULT_DEFAULT_ROLE);
	}

	public FalkorSecurityContext(User user, Set<Role> roles, Set<Privilege> privileges, @Nullable String defaultRole) {
		this.user = Objects.requireNonNull(user, "user must not be null");
		this.effectiveRoles = Collections.unmodifiableSet(computeEffectiveRoleNames(roles, defaultRole));
		this.privileges = Collections.unmodifiableSet(new HashSet<>(privileges));
	}

	public FalkorSecurityContext(User user, java.util.Collection<String> effectiveRoleNames, Set<Privilege> privileges) {
		this(user, effectiveRoleNames, privileges, DEFAULT_DEFAULT_ROLE);
	}

	public FalkorSecurityContext(User user, java.util.Collection<String> effectiveRoleNames, Set<Privilege> privileges,
			@Nullable String defaultRole) {
		this.user = Objects.requireNonNull(user, "user must not be null");
		Set<String> names = new HashSet<>();
		if (effectiveRoleNames != null) {
			names.addAll(effectiveRoleNames);
		}
		if (StringUtils.hasText(defaultRole)) {
			names.add(defaultRole);
		}
		this.effectiveRoles = Collections.unmodifiableSet(names);
		this.privileges = Collections.unmodifiableSet(new HashSet<>(privileges));
	}

	public User getUser() {
		return this.user;
	}

	public Set<String> getEffectiveRoles() {
		return this.effectiveRoles;
	}

	public Set<Privilege> getPrivileges() {
		return this.privileges;
	}

	public boolean hasRole(String roleName) {
		return this.effectiveRoles.contains(roleName);
	}

	public boolean can(Action action, String resource) {
		boolean granted = false;
		for (Privilege privilege : this.privileges) {
			if (privilege.getAction() == action && resourceMatches(privilege.getResource(), resource)) {
				if (privilege.isGrant()) {
					granted = true;
				}
				else {
					// Explicit deny wins
					return false;
				}
			}
		}
		return granted;
	}

	private boolean resourceMatches(String privilegeResource, String requestedResource) {
		if (privilegeResource == null || requestedResource == null) {
			return false;
		}
		if ("*".equals(privilegeResource)) {
			return true;
		}
		// Matches any property on a specific resource, e.g. com.foo.Entity.*
		if (privilegeResource.endsWith(".*")) {
			String prefix = privilegeResource.substring(0, privilegeResource.length() - 2);
			return requestedResource.startsWith(prefix + ".");
		}
		// Default: exact match
		return privilegeResource.equals(requestedResource);
	}

	private Set<String> computeEffectiveRoleNames(@Nullable Set<Role> roles, @Nullable String defaultRole) {
		Set<String> names = new HashSet<>();
		if (StringUtils.hasText(defaultRole)) {
			names.add(defaultRole);
		}
		if (roles == null || roles.isEmpty()) {
			return names;
		}

		Set<Role> visited = new HashSet<>();
		Deque<Role> stack = new ArrayDeque<>(roles);
		while (!stack.isEmpty()) {
			Role role = stack.pop();
			if (role == null || !visited.add(role)) {
				continue;
			}
			if (StringUtils.hasText(role.getName())) {
				names.add(role.getName());
			}
			Set<Role> parents = role.getParentRoles();
			if (parents != null && !parents.isEmpty()) {
				stack.addAll(parents);
			}
		}
		return names;
	}

}
