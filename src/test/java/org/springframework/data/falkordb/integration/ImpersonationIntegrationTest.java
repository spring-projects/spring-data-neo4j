/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.falkordb.core.DefaultFalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBMappingContext;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContextHolder;
import org.springframework.data.falkordb.security.model.Action;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;
import org.springframework.data.falkordb.security.session.FalkorDBSecuritySession;
import org.springframework.data.mapping.model.EntityInstantiators;

import com.falkordb.Driver;
import com.falkordb.impl.api.DriverImpl;

class ImpersonationIntegrationTest {

	private AnnotationConfigApplicationContext context;
	private FalkorDBTemplate template;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(TestConfig.class);
		template = context.getBean(FalkorDBTemplate.class);

		cleanupSecurityGraph();
		seedSecurityGraph();
	}

	@AfterEach
	void tearDown() {
		FalkorSecurityContextHolder.clearContext();
		cleanupSecurityGraph();
		if (context != null) {
			context.close();
		}
	}

	@Test
	void adminShouldBeAbleToImpersonateUserAndGetThatUsersPrivileges() {
		FalkorDBSecuritySession session = new FalkorDBSecuritySession(template, "admin", "PUBLIC");

		// Set an admin context (not necessarily loaded from graph).
		User admin = new User();
		admin.setUsername("root");
		FalkorSecurityContext adminCtx = new FalkorSecurityContext(admin,
				java.util.Set.of("admin"), java.util.Set.of(), "PUBLIC");
		FalkorSecurityContextHolder.setContext(adminCtx);

		try (FalkorSecurityContextHolder.Scope scope = session.impersonate("alice")) {
			FalkorSecurityContext ctx = FalkorSecurityContextHolder.getContext();
			assertThat(ctx).isNotNull();
			assertThat(ctx.getUser().getUsername()).isEqualTo("alice");
			assertThat(ctx.can(Action.READ, RlsDocument.class.getName())).isTrue();
			assertThat(ctx.can(Action.WRITE, RlsDocument.class.getName())).isFalse();
		}

		assertThat(FalkorSecurityContextHolder.getContext().getUser().getUsername()).isEqualTo("root");
	}

	private void seedSecurityGraph() {
		Role userRole = new Role();
		userRole.setName("ROLE_USER");
		template.save(userRole);

		User alice = new User();
		alice.setUsername("alice");
		alice.setActive(true);
		template.save(alice);

		Privilege read = new Privilege();
		read.setAction(Action.READ);
		read.setResource(RlsDocument.class.getName());
		read.setGrant(true);
		template.save(read);

		Map<String, Object> params = new HashMap<>();
		params.put("username", "alice");
		params.put("role", "ROLE_USER");
		params.put("action", "READ");
		params.put("resource", RlsDocument.class.getName());

		// Assign role to user.
		template.query(
				"MATCH (u:_Security_User {username: $username}), (r:_Security_Role {name: $role}) MERGE (u)-[:HAS_ROLE]->(r)",
				params, r -> null);

		// Link privilege to role.
		template.query(
				"MATCH (p:_Security_Privilege {action: $action, resource: $resource, grant: true}), (r:_Security_Role {name: $role}) MERGE (p)-[:GRANTED_TO]->(r)",
				params, r -> null);
	}

	private void cleanupSecurityGraph() {
		// Detach delete all security nodes to avoid constraint errors.
		template.query("MATCH (n:_Security_User) DETACH DELETE n", Collections.emptyMap(), r -> null);
		template.query("MATCH (n:_Security_Role) DETACH DELETE n", Collections.emptyMap(), r -> null);
		template.query("MATCH (n:_Security_Privilege) DETACH DELETE n", Collections.emptyMap(), r -> null);
	}

	@Configuration
	static class TestConfig {

		@Bean
		public Driver falkorDBDriver() {
			return new DriverImpl("localhost", 6379);
		}

		@Bean
		public FalkorDBClient falkorDBClient(Driver driver) {
			return new DefaultFalkorDBClient(driver, "test_impersonation");
		}

		@Bean
		public FalkorDBMappingContext falkorDBMappingContext() {
			return new DefaultFalkorDBMappingContext();
		}

		@Bean
		public FalkorDBTemplate falkorDBTemplate(FalkorDBClient client, FalkorDBMappingContext mappingContext) {
			EntityInstantiators instantiators = new EntityInstantiators();
			DefaultFalkorDBEntityConverter converter = new DefaultFalkorDBEntityConverter(mappingContext, instantiators,
					client);
			return new FalkorDBTemplate(client, mappingContext, converter);
		}

	}

}
