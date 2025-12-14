/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
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

class RlsSecurityIntegrationTest {

	private AnnotationConfigApplicationContext context;
	private RlsDocumentRepository repository;
	private FalkorDBTemplate template;

	private Long aliceId;
	private Long bobId;

	@BeforeEach
	void setUp() {
		context = new AnnotationConfigApplicationContext(RlsTestConfig.class);
		repository = context.getBean(RlsDocumentRepository.class);
		template = context.getBean(FalkorDBTemplate.class);

		// Ensure clean graph state.
		template.deleteAll(RlsDocument.class);

		// Prepare test data directly through the template (no security wrapper).
		RlsDocument alice = template.save(new RlsDocument(null, "A", "alice"));
		RlsDocument bob = template.save(new RlsDocument(null, "B", "bob"));
		aliceId = alice.getId();
		bobId = bob.getId();
	}

	@AfterEach
	void tearDown() {
		try {
			// Cleanup directly through the template.
			template.query("MATCH (n:SecureDocument) DELETE n", Collections.emptyMap(), r -> null);
		}
		finally {
			FalkorSecurityContextHolder.clearContext();
			if (context != null) {
				context.close();
			}
		}
	}

	@Test
	void shouldEnforceRowLevelSecurityOnFindCountExistsAndPageTotals() {
		setUserContext("alice");

		List<String> owners = repository.findAll().stream().map(RlsDocument::getOwner).collect(Collectors.toList());
		assertThat(owners).containsExactly("alice");

		assertThat(repository.count()).isEqualTo(1);
		assertThat(repository.existsById(aliceId)).isTrue();
		assertThat(repository.existsById(bobId)).isFalse();

		var page = repository.findAll(PageRequest.of(0, 10));
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent()).hasSize(1);
	}

	private void setUserContext(String username) {
		Role role = new Role();
		role.setName("ROLE_USER");

		User user = new User();
		user.setUsername(username);
		user.setRoles(Collections.singleton(role));

		Privilege read = new Privilege();
		read.setAction(Action.READ);
		read.setResource(RlsDocument.class.getName());
		read.setGrant(true);

		FalkorSecurityContext ctx = new FalkorSecurityContext(user, Collections.singleton(role), Collections.singleton(read));
		FalkorSecurityContextHolder.setContext(ctx);
	}


	@Configuration
	@EnableFalkorDBRepositories(
			basePackageClasses = RlsDocumentRepository.class,
			repositoryFactoryBeanClass = org.springframework.data.falkordb.security.repository.FalkorDBSecurityRepositoryFactoryBean.class)
	static class RlsTestConfig {

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
