/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

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
 * Integration test for basic row-level security filtering using
 * @RowLevelSecurity(filter = "owner == principal.username").
 */
class RowLevelSecurityIntegrationTest {

	private AnnotationConfigApplicationContext context;

	private RlsDocumentRepository repository;

	private FalkorDBTemplate template;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(RowLevelSecurityTestConfig.class);
		repository = context.getBean(RlsDocumentRepository.class);
		template = context.getBean(FalkorDBTemplate.class);
	}

	@AfterEach
	void tearDown() {
		FalkorSecurityContextHolder.clearContext();
		if (context != null) {
			context.close();
		}
	}

	@Test
	void shouldReturnOnlyDocumentsOwnedByCurrentUser() {
		// Clear any existing documents
		template.deleteAll(RlsDocument.class);

		// Seed two documents: one for alice, one for bob
		RlsDocument aliceDoc = new RlsDocument(null, "doc1", "alice");
		RlsDocument bobDoc = new RlsDocument(null, "doc2", "bob");
		template.save(aliceDoc);
		template.save(bobDoc);

		// given a user alice with READ privilege on SecureDocument
		Role role = new Role();
		role.setName("ROLE_USER");

		User alice = new User();
		alice.setUsername("alice");
		alice.setRoles(Collections.singleton(role));

		Privilege read = new Privilege();
		read.setAction(Action.READ);
		read.setResource(RlsDocument.class.getName());
		read.setGrant(true);

		FalkorSecurityContext ctx = new FalkorSecurityContext(alice,
				Collections.singleton(role), Collections.singleton(read));
		FalkorSecurityContextHolder.setContext(ctx);

		List<RlsDocument> visible = repository.findAll();

		assertThat(visible)
				.hasSize(1)
				.first()
				.extracting(RlsDocument::getOwner)
				.isEqualTo("alice");
	}

	@Configuration
	@EnableFalkorDBRepositories(
			basePackageClasses = RlsDocumentRepository.class,
			repositoryFactoryBeanClass = org.springframework.data.falkordb.security.repository.FalkorDBSecurityRepositoryFactoryBean.class)
	static class RowLevelSecurityTestConfig {

		@Bean
		public Driver falkorDBDriver() {
			return new DriverImpl("localhost", 6379);
		}

		@Bean
		public FalkorDBClient falkorDBClient(Driver driver) {
			return new DefaultFalkorDBClient(driver, "test_rls_integration");
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
