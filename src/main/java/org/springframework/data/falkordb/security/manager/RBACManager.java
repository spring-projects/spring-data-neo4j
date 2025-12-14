/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContextHolder;
import org.springframework.data.falkordb.security.model.Action;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;
import org.springframework.util.Assert;

/**
 * Administrative API for managing RBAC metadata (users, roles, privileges)
 * stored in FalkorDB.
 *
 * All mutating operations require the current {@link FalkorSecurityContext}
 * to contain the configured admin role.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class RBACManager {

	private final FalkorDBTemplate template;

	private final String adminRoleName;

	public RBACManager(FalkorDBTemplate template, String adminRoleName) {
		Assert.notNull(template, "FalkorDBTemplate must not be null");
		Assert.hasText(adminRoleName, "adminRoleName must not be empty");
		this.template = template;
		this.adminRoleName = adminRoleName;
	}

	private void requireAdmin() {
		FalkorSecurityContext ctx = FalkorSecurityContextHolder.getContext();
		if (ctx == null || !ctx.hasRole(this.adminRoleName)) {
			throw new SecurityException("Admin role '" + this.adminRoleName + "' required");
		}
	}

	// -------------------------------------------------------------------------
	// User management
	// -------------------------------------------------------------------------

	public User createUser(String username, String email, List<String> roles) {
		requireAdmin();
		Assert.hasText(username, "username must not be empty");

		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setActive(true);

		User saved = this.template.save(user);

		if (roles != null && !roles.isEmpty()) {
			assignRoles(saved.getUsername(), roles);
		}

		return getUser(username);
	}

	public User updateUser(String username, String email, Boolean active) {
		requireAdmin();
		User existing = getUser(username);
		if (existing == null) {
			return null;
		}
		if (email != null) {
			existing.setEmail(email);
		}
		if (active != null) {
			existing.setActive(active);
		}
		this.template.save(existing);
		return existing;
	}

	public void deleteUser(String username) {
		requireAdmin();
		String cypher = "MATCH (u:_Security_User {username: $username}) DETACH DELETE u";
		this.template.query(cypher, Collections.singletonMap("username", username), FalkorDBClient.QueryResult::records);
	}

	public List<User> listUsers(boolean activeOnly) {
		requireAdmin();
		String cypher = activeOnly
				? "MATCH (u:_Security_User) WHERE u.active = true RETURN u"
				: "MATCH (u:_Security_User) RETURN u";
		return this.template.query(cypher, Collections.emptyMap(), User.class);
	}

	public User getUser(String username) {
		requireAdmin();
		String cypher = "MATCH (u:_Security_User {username: $username}) RETURN u";
		return this.template.queryForObject(cypher,
				Collections.singletonMap("username", username), User.class).orElse(null);
	}

	// -------------------------------------------------------------------------
	// Role management
	// -------------------------------------------------------------------------

	public Role createRole(String name, String description, List<String> parentRoles, boolean immutable) {
		requireAdmin();
		Assert.hasText(name, "name must not be empty");

		Role role = new Role();
		role.setName(name);
		role.setDescription(description);
		role.setImmutable(immutable);

		Role saved = this.template.save(role);

		if (parentRoles != null && !parentRoles.isEmpty()) {
			String cypher = "MATCH (r:_Security_Role {name: $name}), (p:_Security_Role) "
					+ "WHERE p.name IN $parents CREATE (r)-[:INHERITS_FROM]->(p)";
			Map<String, Object> params = new HashMap<>();
			params.put("name", name);
			params.put("parents", parentRoles);
			this.template.query(cypher, params, FalkorDBClient.QueryResult::records);
		}

		return getRole(name);
	}

	public Role updateRole(String name, String description, List<String> parentRoles) {
		requireAdmin();
		Role role = getRole(name);
		if (role == null || role.isImmutable()) {
			return role;
		}
		if (description != null) {
			role.setDescription(description);
		}
		this.template.save(role);
		if (parentRoles != null) {
			String cypher = "MATCH (r:_Security_Role {name: $name})-[rel:INHERITS_FROM]->() DELETE rel";
			this.template.query(cypher, Collections.singletonMap("name", name), FalkorDBClient.QueryResult::records);
			if (!parentRoles.isEmpty()) {
				Map<String, Object> params = new HashMap<>();
				params.put("name", name);
				params.put("parents", parentRoles);
				String createRel = "MATCH (r:_Security_Role {name: $name}), (p:_Security_Role) "
						+ "WHERE p.name IN $parents CREATE (r)-[:INHERITS_FROM]->(p)";
				this.template.query(createRel, params, FalkorDBClient.QueryResult::records);
			}
		}
		return getRole(name);
	}

	public void deleteRole(String name) {
		requireAdmin();
		String cypher = "MATCH (r:_Security_Role {name: $name}) DETACH DELETE r";
		this.template.query(cypher, Collections.singletonMap("name", name), FalkorDBClient.QueryResult::records);
	}

	public List<Role> listRoles() {
		requireAdmin();
		String cypher = "MATCH (r:_Security_Role) RETURN r";
		return this.template.query(cypher, Collections.emptyMap(), Role.class);
	}

	public Role getRole(String name) {
		requireAdmin();
		String cypher = "MATCH (r:_Security_Role {name: $name}) RETURN r";
		return this.template.queryForObject(cypher,
				Collections.singletonMap("name", name), Role.class).orElse(null);
	}

	// -------------------------------------------------------------------------
	// Role assignments
	// -------------------------------------------------------------------------

	public void assignRole(String username, String roleName) {
		requireAdmin();
		Map<String, Object> params = new HashMap<>();
		params.put("username", username);
		params.put("roleName", roleName);
		String cypher = "MATCH (u:_Security_User {username: $username}), (r:_Security_Role {name: $roleName}) "
				+ "MERGE (u)-[:HAS_ROLE]->(r)";
		this.template.query(cypher, params, FalkorDBClient.QueryResult::records);
	}

	public void revokeRole(String username, String roleName) {
		requireAdmin();
		Map<String, Object> params = new HashMap<>();
		params.put("username", username);
		params.put("roleName", roleName);
		String cypher = "MATCH (u:_Security_User {username: $username})-[rel:HAS_ROLE]->(r:_Security_Role {name: $roleName}) "
				+ "DELETE rel";
		this.template.query(cypher, params, FalkorDBClient.QueryResult::records);
	}

	public List<String> getUserRoles(String username) {
		requireAdmin();
		String cypher = "MATCH (u:_Security_User {username: $username})-[:HAS_ROLE]->(r:_Security_Role) RETURN r";
		List<Role> roles = this.template.query(cypher,
				Collections.singletonMap("username", username), Role.class);
		List<String> names = new ArrayList<>();
		for (Role r : roles) {
			if (r.getName() != null) {
				names.add(r.getName());
			}
		}
		return names;
	}

	private void assignRoles(String username, List<String> roles) {
		for (String roleName : roles) {
			assignRole(username, roleName);
		}
	}

	// -------------------------------------------------------------------------
	// Privilege management (MVP)
	// -------------------------------------------------------------------------

	public Privilege grantPrivilege(String roleName, String action, String resource) {
		requireAdmin();
		Action act = Action.valueOf(action);
		Privilege p = new Privilege();
		p.setAction(act);
		p.setResource(resource);
		p.setGrant(true);
		Privilege saved = this.template.save(p);

		Map<String, Object> params = new HashMap<>();
		params.put("roleName", roleName);
		params.put("pid", saved.getId());
		String cypher = "MATCH (r:_Security_Role {name: $roleName}), (p:_Security_Privilege) "
				+ "WHERE id(p) = $pid MERGE (p)-[:GRANTED_TO]->(r)";
		this.template.query(cypher, params, FalkorDBClient.QueryResult::records);
		return saved;
	}

	public Privilege denyPrivilege(String roleName, String action, String resource) {
		requireAdmin();
		Action act = Action.valueOf(action);
		Privilege p = new Privilege();
		p.setAction(act);
		p.setResource(resource);
		p.setGrant(false);
		Privilege saved = this.template.save(p);

		Map<String, Object> params = new HashMap<>();
		params.put("roleName", roleName);
		params.put("pid", saved.getId());
		String cypher = "MATCH (r:_Security_Role {name: $roleName}), (p:_Security_Privilege) "
				+ "WHERE id(p) = $pid MERGE (p)-[:GRANTED_TO]->(r)";
		this.template.query(cypher, params, FalkorDBClient.QueryResult::records);
		return saved;
	}

	public void revokePrivilege(Long privilegeId) {
		requireAdmin();
		String cypher = "MATCH (p:_Security_Privilege) WHERE id(p) = $pid DETACH DELETE p";
		this.template.query(cypher, Collections.singletonMap("pid", privilegeId), FalkorDBClient.QueryResult::records);
	}

	public List<Privilege> listPrivileges(String roleName) {
		requireAdmin();
		String cypher = "MATCH (r:_Security_Role {name: $roleName})<-[:GRANTED_TO]-(p:_Security_Privilege) RETURN p";
		return this.template.query(cypher,
				Collections.singletonMap("roleName", roleName), Privilege.class);
	}

}
