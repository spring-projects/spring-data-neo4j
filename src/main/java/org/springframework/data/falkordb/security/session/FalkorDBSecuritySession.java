/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContextHolder;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.User;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Convenience API for working with graph-stored RBAC.
 *
 * Provides:
 * - creating a {@link FalkorSecurityContext} for a given username
 * - try-with-resources scopes for running code as a user
 * - admin-only impersonation scopes
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class FalkorDBSecuritySession {

	private final FalkorDBTemplate template;
	private final String adminRole;
	private final @Nullable String defaultRole;

	public FalkorDBSecuritySession(FalkorDBTemplate template, String adminRole, @Nullable String defaultRole) {
		this.template = Objects.requireNonNull(template, "template must not be null");
		Assert.hasText(adminRole, "adminRole must not be empty");
		this.adminRole = adminRole;
		this.defaultRole = defaultRole;
	}

	public FalkorSecurityContext loadContext(String username) {
		Assert.hasText(username, "username must not be empty");

		User user = loadUser(username);
		if (user == null || !user.isActive()) {
			return null;
		}

		Set<String> roleNames = loadRoleNames(username);
		roleNames.addAll(resolveInheritedRoleNames(roleNames));

		Set<Privilege> privileges = loadPrivileges(roleNames);
		return new FalkorSecurityContext(user, roleNames, privileges, this.defaultRole);
	}

	public FalkorSecurityContextHolder.Scope runAs(String username) {
		FalkorSecurityContext ctx = loadContext(username);
		if (ctx == null) {
			throw new IllegalArgumentException("Unknown or inactive user: " + username);
		}
		return FalkorSecurityContextHolder.withContext(ctx);
	}

	public FalkorSecurityContextHolder.Scope impersonate(String username) {
		requireAdmin();
		return runAs(username);
	}

	private void requireAdmin() {
		FalkorSecurityContext ctx = FalkorSecurityContextHolder.getContext();
		if (ctx == null || !ctx.hasRole(this.adminRole)) {
			throw new SecurityException("Admin role '" + this.adminRole + "' required");
		}
	}

	@Nullable
	private User loadUser(String username) {
		String cypher = "MATCH (n:_Security_User {username: $username}) RETURN n, id(n) as nodeId";
		return this.template.query(cypher, Collections.singletonMap("username", username), result -> {
			for (FalkorDBClient.Record record : result.records()) {
				return this.template.getConverter().read(User.class, record);
			}
			return null;
		});
	}

	private Set<String> loadRoleNames(String username) {
		Set<String> roles = new HashSet<>();
		if (StringUtils.hasText(this.defaultRole)) {
			roles.add(this.defaultRole);
		}

		String cypher = "MATCH (u:_Security_User {username: $username})-[:HAS_ROLE]->(r:_Security_Role) "
				+ "RETURN DISTINCT r.name as roleName";

		return this.template.query(cypher, Collections.singletonMap("username", username), result -> {
			for (FalkorDBClient.Record record : result.records()) {
				Object rn = record.get("roleName");
				if (rn != null) {
					roles.add(String.valueOf(rn));
				}
			}
			return roles;
		});
	}

	private Set<String> resolveInheritedRoleNames(Set<String> roleNames) {
		if (roleNames == null || roleNames.isEmpty()) {
			return Collections.emptySet();
		}
		try {
			String cypher = "MATCH (r:_Security_Role) WHERE r.name IN $roleNames "
					+ "OPTIONAL MATCH (r)-[:INHERITS_FROM*0..]->(p:_Security_Role) "
					+ "RETURN DISTINCT p.name as roleName";
			Map<String, Object> params = new HashMap<>();
			params.put("roleNames", List.copyOf(roleNames));
			return this.template.query(cypher, params, result -> {
				Set<String> names = new HashSet<>();
				for (FalkorDBClient.Record record : result.records()) {
					Object rn = record.get("roleName");
					if (rn != null) {
						names.add(String.valueOf(rn));
					}
				}
				return names;
			});
		}
		catch (Exception ignored) {
			return Collections.emptySet();
		}
	}

	private Set<Privilege> loadPrivileges(Set<String> roleNames) {
		if (roleNames == null || roleNames.isEmpty()) {
			return Collections.emptySet();
		}
		String cypher = "MATCH (r:_Security_Role)<-[:GRANTED_TO]-(p:_Security_Privilege) "
				+ "WHERE r.name IN $roleNames RETURN p as n, id(p) as nodeId";
		List<Privilege> list = this.template.query(cypher,
				Collections.singletonMap("roleNames", List.copyOf(roleNames)), Privilege.class);
		return new HashSet<>(list);
	}

}
