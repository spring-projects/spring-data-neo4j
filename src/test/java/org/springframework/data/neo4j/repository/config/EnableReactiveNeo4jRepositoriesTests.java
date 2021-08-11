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
package org.springframework.data.neo4j.repository.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Michael J. Simons
 */
class EnableReactiveNeo4jRepositoriesTests {

	@ExtendWith({ SpringExtension.class })
	@ContextConfiguration(classes = EnableReactiveNeo4jRepositoriesTests.ExcludeFilterShouldWork.Config.class)
	static class ExcludeFilterShouldWork {

		@Test
		void test(@Autowired ObjectProvider<RepositoryToBeExcluded> repos) {
			assertThat(repos.iterator()).isExhausted();
		}

		interface RepositoryToBeExcluded extends ReactiveNeo4jRepository<AnEntity, String> {
		}

		@Configuration
		@EnableReactiveNeo4jRepositories(considerNestedRepositories = true, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RepositoryToBeExcluded.class))
		static class Config extends AbstractNeo4jConfig {

			@Bean
			@Override
			public Driver driver() {
				return Mockito.mock(Driver.class);
			}
		}
	}
}
