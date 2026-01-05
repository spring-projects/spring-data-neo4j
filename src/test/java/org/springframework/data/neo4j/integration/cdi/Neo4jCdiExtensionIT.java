/*
 * Copyright 2011-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.cdi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.driver.Driver;
import org.springframework.data.neo4j.config.Neo4jCdiExtension;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.test.Neo4jExtension;

/**
 * @author Michael J. Simons
 * @soundtrack Various - TRON Legacy R3conf1gur3d
 */
@ExtendWith(Neo4jExtension.class)
class Neo4jCdiExtensionIT {

	protected static Neo4jExtension.Neo4jConnectionSupport connectionSupport;

	@ApplicationScoped
	static class RealDriverFactory {

		@Produces
		@Singleton
		public Driver driver() {
			return connectionSupport.getDriver();
		}

		@Produces
		@Singleton
		public Configuration cypherDslConfiguration() {
			if (connectionSupport.isCypher5SyntaxCompatible()) {
				return Configuration.newConfig().withDialect(Dialect.NEO4J_5).build();
			}

			return Configuration.newConfig().withDialect(Dialect.NEO4J_4).build();
		}
	}

	@ApplicationScoped
	static class MockedDriverFactory {

		@Produces
		@Singleton
		public Driver driver() {
			return Mockito.mock(Driver.class);
		}
	}

	@ApplicationScoped
	static class CustomDependencyProducer {

		Neo4jConversions conversions = Mockito.mock(Neo4jConversions.class);

		DatabaseSelectionProvider databaseSelectionProvider = Mockito.mock(DatabaseSelectionProvider.class);

		Neo4jOperations neo4jOperations = Mockito.mock(Neo4jOperations.class);

		@Produces @Singleton
		public Neo4jConversions getConversions() {
			return conversions;
		}

		@Produces @Singleton
		public DatabaseSelectionProvider getDatabaseSelectionProvider() {
			return databaseSelectionProvider;
		}

		@Produces @Singleton
		public Neo4jOperations getNeo4jOperations() {
			return neo4jOperations;
		}
	}

	@ApplicationScoped
	static class BrokenCustomDependencyProducer {

		@Produces @Singleton
		public Neo4jConversions getConversions1() {
			return Mockito.mock(Neo4jConversions.class);
		}

		@Produces @Singleton
		public Neo4jConversions getConversions2() {
			return Mockito.mock(Neo4jConversions.class);
		}
	}

	@Test
	void cdiExtensionShouldProduceFunctionalRepositories() {

		try (SeContainer container = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addExtensions(Neo4jCdiExtension.class)
				.addBeanClasses(RealDriverFactory.class, PersonRepository.class, Neo4jBasedService.class)
				.initialize()) {
			Neo4jBasedService client = container
					.select(Neo4jBasedService.class).get();

			assertThat(client).isNotNull();
			assertThat(client.driver).isNotNull();
			assertThat(client.personRepository).isNotNull();

			Person p = client.personRepository.save(new Person("Hello"));
			assertThat(p.getId()).isNotNull();

			Optional<Person> loadedPerson = client.personRepository.findById(p.getId());
			assertThat(loadedPerson).isPresent().hasValueSatisfying(v -> v.getId().equals(p.getId()));
		}
	}

	@Test
	void shouldAllowToOverrideASetOfDependents() {

		Class<?> configurationSupport = getNeo4jCdiConfigurationSupport();
		try (SeContainer container = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses(
						MockedDriverFactory.class,
						CustomDependencyProducer.class,
						configurationSupport
				)
				.initialize()) {

			CustomDependencyProducer customDependencyProducer = container.select(CustomDependencyProducer.class).get();

			assertThat(container.select(Neo4jConversions.class).get())
					.isEqualTo(customDependencyProducer.getConversions());
			assertThat(container.select(DatabaseSelectionProvider.class).get())
					.isEqualTo(customDependencyProducer.getDatabaseSelectionProvider());
			assertThat(container.select(Neo4jOperations.class).get())
					.isEqualTo(customDependencyProducer.getNeo4jOperations());
		}
	}

	@Test
	void shouldRequireUniqueDefaultBeans() {

		Class<?> configurationSupport = getNeo4jCdiConfigurationSupport();
		try (SeContainer container = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses(
						MockedDriverFactory.class,
						BrokenCustomDependencyProducer.class,
						configurationSupport
				)
				.initialize()) {

			assertThatExceptionOfType(AmbiguousResolutionException.class).isThrownBy(() -> {
				Neo4jMappingContext context = container.select(Neo4jMappingContext.class).get();
			});

		}
	}

	private Class<?> getNeo4jCdiConfigurationSupport() {
		try {
			// Wrapped in a reflection call so that we don't need to make it public just
			// for testing it's producer methods.
			return Class.forName("org.springframework.data.neo4j.config.Neo4jCdiConfigurationSupport");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("¯\\_(ツ)_/¯", e);
		}
	}
}
