/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.imperative;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.core.Neo4jOperations;
import org.neo4j.springframework.data.integration.shared.PersonWithAllConstructor;
import org.neo4j.springframework.data.repository.Neo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.neo4j.springframework.data.repository.support.Neo4jEntityInformation;
import org.neo4j.springframework.data.repository.support.SimpleNeo4jRepository;
import org.neo4j.springframework.data.test.DriverMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Make sure custom base repositories can be used.
 *
 * @author Michael J. Simons
 */
@ExtendWith({ SpringExtension.class })
public class CustomBaseRepositoryIT {

	@Test
	public void customBaseRepositoryShouldBeInUse(@Autowired MyPersonRepository repository) {

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> repository.findAll())
			.withMessage("This implementation does not support `findAll`.");
	}

	@Configuration
	@EnableNeo4jRepositories(
		repositoryBaseClass = MyRepositoryImpl.class,
		considerNestedRepositories = true,
		includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = MyPersonRepository.class)
	)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return DriverMocks.withOpenSessionAndTransaction();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}
	}

	interface MyPersonRepository extends Neo4jRepository<PersonWithAllConstructor, Long> {
	}

	static class MyRepositoryImpl<T, ID> extends SimpleNeo4jRepository<T, ID> {

		MyRepositoryImpl(Neo4jOperations neo4jOperations, Neo4jEntityInformation<T, ID> entityInformation) {
			super(neo4jOperations, entityInformation);

			assertThat(neo4jOperations).isNotNull();
			assertThat(entityInformation).isNotNull();
			assertThat(entityInformation.getEntityMetaData().getUnderlyingClass())
				.isEqualTo(PersonWithAllConstructor.class);
		}

		@Override
		public List<T> findAll() {
			throw new UnsupportedOperationException("This implementation does not support `findAll`.");
		}
	}
}
