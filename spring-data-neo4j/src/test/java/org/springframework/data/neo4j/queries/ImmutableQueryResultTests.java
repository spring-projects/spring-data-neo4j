/*
 * Copyright (c) 2018 "Neo4j, Inc." / "Pivotal Software, Inc."
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.queries;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.queries.immutable_query_result.ImmutableQueryResult;
import org.springframework.data.neo4j.queries.immutable_query_result.ImmutableQueryResultWithNonFinalFields;
import org.springframework.data.neo4j.queries.immutable_query_result.ImmutableQueryResultWithWithers;
import org.springframework.data.neo4j.queries.immutable_query_result.SomeNodeRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { ImmutableQueryResultTests.ContextConfig.class })
@RunWith(SpringRunner.class)
public class ImmutableQueryResultTests extends MultiDriverTestClass {

	@Autowired private SomeNodeRepository someNodeRepository;

	@Test
	public void shouldSupportImmutableQueryResults() {
		List<ImmutableQueryResult> x = someNodeRepository.findImmutableQueryResults();
		assertThat(x)
				.flatExtracting(ImmutableQueryResult::getName, ImmutableQueryResult::getNumber)
				.contains("James Bond", 7L);
	}

	@Test
	public void shouldNotMutateQueryResultsThroughFieldAccess() {
		List<ImmutableQueryResultWithNonFinalFields> x = someNodeRepository.findImmutableQueryResultsWithNonFinalFields();
		assertThat(x)
				.flatExtracting(ImmutableQueryResultWithNonFinalFields::getName, ImmutableQueryResultWithNonFinalFields::getNumber)
				.contains("James Bond", 7L);
	}

	@Test
	public void shouldUseWithers() {
		List<ImmutableQueryResultWithWithers> x = someNodeRepository.findImmutableQueryResultsWithWithers();
		assertThat(x)
				.flatExtracting(ImmutableQueryResultWithWithers::getName, ImmutableQueryResultWithWithers::getNumber)
				.contains("James Bond", 7L);
	}

	private void executeUpdate(String cypher) {
		getGraphDatabaseService().execute(cypher);
	}

	@Configuration
	@ComponentScan(basePackageClasses = SomeNodeRepository.class)
	@EnableNeo4jRepositories(basePackageClasses = SomeNodeRepository.class)
	@EnableTransactionManagement
	static class ContextConfig {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), ImmutableQueryResult.class.getPackage().getName());
		}

	}
}
