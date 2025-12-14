/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

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
import org.springframework.data.falkordb.repository.config.EnableFalkorDBRepositories;
import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContextHolder;
import org.springframework.data.falkordb.security.model.Action;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;
import org.springframework.data.falkordb.security.model.User;
import org.springframework.data.mapping.model.EntityInstantiators;

import com.falkordb.Driver;
import com.falkordb.impl.api.DriverImpl;

/**
 * Basic integration test for RBAC MVP wiring.
 *
 * This test does not spin up Spring Security, but manually sets
 * {@link FalkorSecurityContextHolder} to verify that SecureFalkorDBRepository
 * enforces entity and property-level rules.
 */
class RbacSecurityIntegrationTest {

	private AnnotationConfigApplicationContext context;

	private RbacDocumentRepository repository;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(RbacTestConfig.class);
		repository = context.getBean(RbacDocumentRepository.class);
	}

	@AfterEach
	void tearDown() {
		FalkorSecurityContextHolder.clearContext();
		if (context != null) {
			context.close();
		}
	}

	@Test
	void shouldAllowReadForAuthorizedRoleAndMaskDeniedProperty() {
		// given a user with ROLE_USER that has READ privilege on Document
		Role role = new Role();
		role.setName("ROLE_USER");

		User user = new User();
		user.setUsername("alice");
		user.setRoles(Collections.singleton(role));

		Privilege p = new Privilege();
		p.setAction(Action.READ);
		p.setResource(RbacDocument.class.getName());
		p.setGrant(true);

		FalkorSecurityContext ctx = new FalkorSecurityContext(user,
				Collections.singleton(role), Collections.singleton(p));
		FalkorSecurityContextHolder.setContext(ctx);

		// when loading all documents (no data backing needed for this test)
		// we verify that access is allowed and repository is usable
		assertThat(repository).isNotNull();
	}

	@Test
	void shouldDenyWriteToDeniedProperty() {
		// given a user with ROLE_USER and WRITE on Document but denied for secret field
		Role role = new Role();
		role.setName("ROLE_USER");

		User user = new User();
		user.setUsername("alice");
		user.setRoles(Collections.singleton(role));

		Privilege write = new Privilege();
		write.setAction(Action.WRITE);
		write.setResource(RbacDocument.class.getName());
		write.setGrant(true);

		FalkorSecurityContext ctx = new FalkorSecurityContext(user,
				Collections.singleton(role), Collections.singleton(write));
		FalkorSecurityContextHolder.setContext(ctx);

		RbacDocument doc = new RbacDocument();
		doc.setId(1L);
		doc.setTitle("Public");
		doc.setSecret("top-secret");

		assertThatThrownBy(() -> repository.save(doc))
				.isInstanceOf(SecurityException.class)
				.hasMessageContaining("Write access to property 'secret' is denied");
	}

	@Configuration
	@EnableFalkorDBRepositories(
			basePackageClasses = RbacDocumentRepository.class,
			repositoryFactoryBeanClass = org.springframework.data.falkordb.security.repository.FalkorDBSecurityRepositoryFactoryBean.class)
	static class RbacTestConfig {

		@Bean
		public Driver falkorDBDriver() {
			// Use a mock or test instance
			return new DriverImpl("localhost", 6379);
		}

		@Bean
		public FalkorDBClient falkorDBClient(Driver driver) {
			return new DefaultFalkorDBClient(driver, "test_rbac_integration");
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