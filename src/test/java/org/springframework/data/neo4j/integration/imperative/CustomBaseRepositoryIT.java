/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.integration.imperative;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.Neo4jEntityInformation;
import org.springframework.data.neo4j.repository.support.SimpleNeo4jRepository;
import org.springframework.data.neo4j.test.DriverMocks;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Make sure custom base repositories can be used.
 *
 * @author Michael J. Simons
 */
@ExtendWith({ SpringExtension.class })
public class CustomBaseRepositoryIT {

	@Test
	public void customBaseRepositoryShouldBeInUse(@Autowired MyPersonRepository repository) {

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findAll())
			.withMessage("This implementation does not support `findAll`");
	}

	interface MyPersonRepository extends Neo4jRepository<PersonWithAllConstructor, Long> {

	}

	/**
	 * Used in the FAQ as well
	 *
	 * @param <T> Type of the entity
	 * @param <ID> Type of the id
	 */
	// tag::custom-base-repository[]
	public static class MyRepositoryImpl<T, ID> extends SimpleNeo4jRepository<T, ID> {

		MyRepositoryImpl(Neo4jOperations neo4jOperations, Neo4jEntityInformation<T, ID> entityInformation) {
			super(neo4jOperations, entityInformation); // <.>
			// end::custom-base-repository[]
			assertThat(neo4jOperations).isNotNull();
			assertThat(entityInformation).isNotNull();
			Assertions.assertThat(entityInformation.getEntityMetaData().getUnderlyingClass())
				.isEqualTo(PersonWithAllConstructor.class);
			// tag::custom-base-repository[]
		}

		@Override
		public List<T> findAll() {
			throw new UnsupportedOperationException("This implementation does not support `findAll`");
		}

	}
	// end::custom-base-repository[]

	@Configuration
	@EnableNeo4jRepositories(repositoryBaseClass = MyRepositoryImpl.class, considerNestedRepositories = true,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = MyPersonRepository.class))
	@EnableTransactionManagement
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return DriverMocks.withOpenSessionAndTransaction();
		}

		@Override
		public boolean isCypher5Compatible() {
			return false; // does not matter
		}

	}

}
