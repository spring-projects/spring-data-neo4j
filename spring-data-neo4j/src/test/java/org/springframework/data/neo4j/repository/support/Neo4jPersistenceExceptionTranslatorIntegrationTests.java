/*
 * Copyright 2011-2020 the original author or authors.
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


import java.time.Year;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.harness.ServerControls;
import org.neo4j.ogm.config.AutoIndexMode;
import org.neo4j.ogm.exception.core.InvalidPropertyFieldException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.neo4j.domain.invalid.EntityWithInvalidProperty;
import org.springframework.data.neo4j.domain.invalid.EntityWithInvalidPropertyRepository;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Michael J. Simons
 * @soundtrack Opeth - Blackwater Park
 */
@ContextConfiguration(classes = Neo4jPersistenceExceptionTranslatorIntegrationTests.ContextConfiguration.class)
@RunWith(SpringRunner.class)
public class Neo4jPersistenceExceptionTranslatorIntegrationTests {

	@Autowired
	private EntityWithInvalidPropertyRepository repository;

	@Test
	public void invalidPropertyFieldExceptionShouldBeTranslated() {

		assertThatExceptionOfType(TypeMismatchDataAccessException.class)
				.isThrownBy(() -> repository
						.save(new EntityWithInvalidProperty(Year.of(2018))))
				.withCauseInstanceOf(InvalidPropertyFieldException.class)
				.withMessageContaining("'org.springframework.data.neo4j.domain.invalid.EntityWithInvalidProperty#year' is not persistable as property but has not been marked as transient.");
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.domain.invalid",
			repositoryPackages = "org.springframework.data.neo4j.domain.invalid")
	static class ContextConfiguration {

		// Turn off Autoindex Manager to avoid eager entity scanning
		@Bean
		org.neo4j.ogm.config.Configuration neo4jOGMConfiguration(ServerControls neo4jTestServer) {
			return new org.neo4j.ogm.config.Configuration.Builder() //
					.uri(neo4jTestServer.boltURI().toString()) //
					.autoIndex(AutoIndexMode.NONE.getName()) //
					.build();
		}

		// Eager entity scanning is also triggered without a Spring conversion service
		@Bean
		public ConversionService conversionService() {
			return DefaultConversionService.getSharedInstance();
		}
	}
}
