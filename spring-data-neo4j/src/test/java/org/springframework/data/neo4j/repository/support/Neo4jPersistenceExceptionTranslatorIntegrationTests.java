/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.repository.support;

import static org.junit.rules.ExpectedException.*;

import java.time.Year;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;
import org.neo4j.ogm.exception.core.InvalidPropertyFieldException;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.neo4j.domain.invalid.EntityWithInvalidProperty;
import org.springframework.data.neo4j.domain.invalid.EntityWithInvalidPropertyRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Opeth - Blackwater Park
 */
@ContextConfiguration(classes = { Neo4jPersistenceExceptionTranslatorIntegrationTests.ContextConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class Neo4jPersistenceExceptionTranslatorIntegrationTests {

	@Rule public final ExpectedException expectedException = none();

	@Autowired private EntityWithInvalidPropertyRepository repository;

	@Test
	public void invalidPropertyFieldExceptionShouldBeTranslated() {

		expectedException.expect(TypeMismatchDataAccessException.class);
		expectedException.expectCause(Matchers.any(InvalidPropertyFieldException.class));
		expectedException.expectMessage(
				"'org.springframework.data.neo4j.domain.invalid.EntityWithInvalidProperty#year' is not persistable as property but has not been marked as transient.");

		repository.save(new EntityWithInvalidProperty(Year.of(2018)));
	}

	@Configuration
	@ComponentScan(basePackageClasses = EntityWithInvalidProperty.class)
	@EnableNeo4jRepositories(basePackageClasses = EntityWithInvalidProperty.class)
	@EnableTransactionManagement
	static class ContextConfiguration {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			// Cannot use the base configuration as it always uses the
			// auto index manager which is currently faulty and doesn't
			// obey "None" completely and insists on a index scan
			// TODO just turn auto index manager off
			return new SessionFactory(new EmbeddedDriver(MultiDriverTestClass.getGraphDatabaseService()),
					EntityWithInvalidProperty.class.getPackage().getName());
		}

		// Eager entity scanning is also triggered without a Spring conversion service
		@Bean
		public ConversionService conversionService() {
			return DefaultConversionService.getSharedInstance();
		}
	}
}
