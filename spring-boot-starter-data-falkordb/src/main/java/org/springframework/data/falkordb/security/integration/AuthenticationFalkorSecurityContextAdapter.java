/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.integration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Adapter that creates a {@link FalkorSecurityContext} from a Spring Security
 * {@link Authentication} by loading the corresponding {@link User} and its
 * associated {@link Role} and {@link Privilege} information from FalkorDB.
 *
 * MVP implementation: loads User by username and does a simple match of
 * role names to Spring authorities; Privileges can be looked up by role
 * name prefix or via a dedicated query.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class AuthenticationFalkorSecurityContextAdapter {

	private final FalkorDBTemplate template;

	private final PrivilegeService privilegeService;

	private final String defaultRole;

	public AuthenticationFalkorSecurityContextAdapter(FalkorDBTemplate template, PrivilegeService privilegeService,
			String defaultRole) {
		this.template = template;
		this.privilegeService = privilegeService;
		this.defaultRole = defaultRole;
	}

	public FalkorSecurityContext fromAuthentication(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		String username = principal instanceof org.springframework.security.core.userdetails.UserDetails u
				? u.getUsername()
				: String.valueOf(principal);

		User user = loadUserByUsername(username);
		if (user == null || !user.isActive()) {
			return null;
		}

		Set<String> springAuthorities = extractAuthorityNames(authentication);
		Set<String> roleNames = extractRoleNames(user, springAuthorities);

		// Expand role inheritance from graph if possible
		roleNames.addAll(resolveInheritedRoleNames(roleNames));

		Set<Privilege> privileges = this.privilegeService.loadPrivileges(username, roleNames);

		return new FalkorSecurityContext(user, roleNames, privileges, this.defaultRole);
	}

	private User loadUserByUsername(String username) {
		String cypher = "MATCH (u:_Security_User {username: $username}) RETURN u as n, id(u) as nodeId";
		return this.template.query(cypher, Collections.singletonMap("username", username), result -> {
			for (org.springframework.data.falkordb.core.FalkorDBClient.Record record : result.records()) {
				User u = this.template.getConverter().read(User.class, record);
				return u;
			}
			return null;
		});
	}


	private Set<String> extractAuthorityNames(Authentication authentication) {
		if (authentication == null) {
			return Collections.emptySet();
		}
		Set<String> names = new HashSet<>();
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority != null && authority.getAuthority() != null) {
				names.add(authority.getAuthority());
			}
		}
		return names;
	}

	private Set<String> extractRoleNames(User user, Set<String> springAuthorities) {
		Set<String> roleNames = new HashSet<>();
		if (user != null && user.getRoles() != null && !user.getRoles().isEmpty()) {
			for (Role role : user.getRoles()) {
				if (role != null && role.getName() != null) {
					roleNames.add(role.getName());
				}
			}
		}
		// If the user has no graph roles, fall back to Spring authorities as role names
		if (roleNames.isEmpty() && springAuthorities != null && !springAuthorities.isEmpty()) {
			roleNames.addAll(springAuthorities);
		}
		// Intersect with Spring authorities if present
		if (springAuthorities != null && !springAuthorities.isEmpty()) {
			roleNames.removeIf(rn -> !springAuthorities.contains(rn));
		}
		if (this.defaultRole != null && !this.defaultRole.isBlank()) {
			roleNames.add(this.defaultRole);
		}
		return roleNames;
	}

	private Set<String> resolveInheritedRoleNames(Set<String> roleNames) {
		if (roleNames == null || roleNames.isEmpty()) {
			return Collections.emptySet();
		}
		try {
			String cypher = "MATCH (r:_Security_Role) WHERE r.name IN $roleNames "
					+ "OPTIONAL MATCH (r)-[:INHERITS_FROM*0..]->(p:_Security_Role) "
					+ "RETURN DISTINCT p";
			List<Role> roles = this.template.query(cypher,
					Collections.singletonMap("roleNames", roleNames), Role.class);
			Set<String> inherited = new HashSet<>();
			for (Role r : roles) {
				if (r != null && r.getName() != null) {
					inherited.add(r.getName());
				}
			}
			return inherited;
		}
		catch (Exception ignored) {
			return Collections.emptySet();
		}
	}

}
