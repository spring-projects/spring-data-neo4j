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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.integration.shared.PersonWithAllConstructor;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.core.Neo4jEntityInformation;
import org.springframework.data.neo4j.repository.support.SimpleReactiveNeo4jRepository;
import org.springframework.data.neo4j.test.DriverMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Make sure custom base repositories can be used in reactive configurations.
 *
 * @author Michael J. Simons
 */
@ExtendWith({ SpringExtension.class })
public class CustomReactiveBaseRepositoryIT {

	@Test
	public void customBaseRepositoryShouldBeInUse(@Autowired MyPersonRepository repository) {

		StepVerifier.create(repository.findAll()).expectErrorMatches(e -> e instanceof UnsupportedOperationException
				&& e.getMessage().equals("This implementation does not support `findAll`."));
	}

	interface MyPersonRepository extends ReactiveNeo4jRepository<PersonWithAllConstructor, Long> {}

	static class MyRepositoryImpl<T, ID> extends SimpleReactiveNeo4jRepository<T, ID> {

		MyRepositoryImpl(ReactiveNeo4jOperations neo4jOperations, Neo4jEntityInformation<T, ID> entityInformation) {
			super(neo4jOperations, entityInformation);

			assertThat(neo4jOperations).isNotNull();
			assertThat(entityInformation).isNotNull();
			Assertions.assertThat(entityInformation.getEntityMetaData().getUnderlyingClass())
					.isEqualTo(PersonWithAllConstructor.class);
		}

		@Override
		public Flux<T> findAll() {
			throw new UnsupportedOperationException("This implementation does not support `findAll`.");
		}
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(repositoryBaseClass = MyRepositoryImpl.class, considerNestedRepositories = true,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = MyPersonRepository.class))
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return DriverMocks.withOpenReactiveSessionAndTransaction();
		}

	}
}
