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

	public AuthenticationFalkorSecurityContextAdapter(FalkorDBTemplate template, PrivilegeService privilegeService) {
		this.template = template;
		this.privilegeService = privilegeService;
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

		Set<Role> roles = new HashSet<>(user.getRoles());
		// Optionally, intersect with Spring Security authorities
		Set<String> springAuthorities = extractAuthorityNames(authentication);
		roles.removeIf(role -> !springAuthorities.isEmpty() && !springAuthorities.contains(role.getName()));

		Set<Privilege> privileges = this.privilegeService.loadPrivileges(username, roles);

		return new FalkorSecurityContext(user, roles, privileges);
	}

	private User loadUserByUsername(String username) {
		String cypher = "MATCH (u:_Security_User {username: $username})-[:HAS_ROLE]->(r:_Security_Role) "
				+ "OPTIONAL MATCH (r)<-[:GRANTED_TO]-(p:_Security_Privilege) "
				+ "RETURN u, collect(DISTINCT r) as roles, collect(DISTINCT p) as privileges";
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

}
