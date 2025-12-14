/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.security.model.Action;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;

/**
 * Falkor-specific security context used for RBAC checks.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class FalkorSecurityContext {

	private final User user;

	private final Set<String> effectiveRoles;

	private final Set<Privilege> privileges;

	public FalkorSecurityContext(User user, Set<Role> roles, Set<Privilege> privileges) {
		this.user = Objects.requireNonNull(user, "user must not be null");
		this.effectiveRoles = Collections.unmodifiableSet(extractRoleNames(roles));
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
		// MVP: exact match; can be extended to patterns later
		return privilegeResource.equals(requestedResource);
	}

	private Set<String> extractRoleNames(Set<Role> roles) {
		if (roles == null || roles.isEmpty()) {
			return Collections.emptySet();
		}
		Set<String> names = new HashSet<>();
		for (Role role : roles) {
			if (role != null && role.getName() != null) {
				names.add(role.getName());
			}
		}
		return names;
	}

}
