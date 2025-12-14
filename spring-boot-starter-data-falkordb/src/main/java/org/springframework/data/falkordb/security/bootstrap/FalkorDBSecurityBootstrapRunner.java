/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.bootstrap;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.data.falkordb.FalkorDBSecurityProperties;
import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.util.StringUtils;

/**
 * Bootstrap runner to create/ensure the initial admin role and an optional admin user.
 *
 * This is intended to get applications started in production environments while
 * still allowing full RBAC management through {@code RBACManager} afterwards.
 */
public class FalkorDBSecurityBootstrapRunner implements ApplicationRunner {

	private final FalkorDBTemplate template;

	private final FalkorDBSecurityProperties properties;

	public FalkorDBSecurityBootstrapRunner(FalkorDBTemplate template, FalkorDBSecurityProperties properties) {
		this.template = template;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		ensureDefaultRole();
		ensureAdminRole();
		ensureAdminUser();
	}

	private void ensureDefaultRole() {
		String role = this.properties.getDefaultRole();
		if (!StringUtils.hasText(role)) {
			return;
		}
		Map<String, Object> params = new HashMap<>();
		params.put("name", role);
		params.put("description", "Bootstrapped default role");
		params.put("createdAt", Instant.now().toString());

		String cypher = "MERGE (r:_Security_Role {name: $name}) "
				+ "ON CREATE SET r.description = $description, r.immutable = true, r.createdAt = $createdAt "
				+ "RETURN r";

		this.template.query(cypher, params, FalkorDBClient.QueryResult::records);
	}

	private void ensureAdminRole() {
		String adminRole = this.properties.getAdminRole();
		if (!StringUtils.hasText(adminRole)) {
			return;
		}

		Map<String, Object> params = new HashMap<>();
		params.put("name", adminRole);
		params.put("description", "Bootstrapped administrative role");
		params.put("createdAt", Instant.now().toString());

		String cypher = "MERGE (r:_Security_Role {name: $name}) "
				+ "ON CREATE SET r.description = $description, r.immutable = true, r.createdAt = $createdAt "
				+ "RETURN r";

		this.template.query(cypher, params, FalkorDBClient.QueryResult::records);
	}

	private void ensureAdminUser() {
		FalkorDBSecurityProperties.Bootstrap bootstrap = this.properties.getBootstrap();
		if (bootstrap == null) {
			return;
		}

		String username = bootstrap.getAdminUsername();
		if (!StringUtils.hasText(username)) {
			return;
		}

		String adminRole = this.properties.getAdminRole();
		if (!StringUtils.hasText(adminRole)) {
			return;
		}

		Map<String, Object> params = new HashMap<>();
		params.put("username", username);
		params.put("email", bootstrap.getAdminEmail());
		params.put("createdAt", Instant.now().toString());
		params.put("adminRole", adminRole);

		String mergeUser = "MERGE (u:_Security_User {username: $username}) "
				+ "ON CREATE SET u.email = $email, u.active = true, u.createdAt = $createdAt "
				+ "RETURN u";
		this.template.query(mergeUser, params, FalkorDBClient.QueryResult::records);

		String roleLink = "MATCH (u:_Security_User {username: $username}), (r:_Security_Role {name: $adminRole}) "
				+ "MERGE (u)-[:HAS_ROLE]->(r)";
		this.template.query(roleLink, params, FalkorDBClient.QueryResult::records);
	}

}