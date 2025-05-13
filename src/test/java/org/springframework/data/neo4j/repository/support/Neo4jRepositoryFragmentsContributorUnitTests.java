/*
 * Copyright 2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithAllAdditionalTypes;
import org.springframework.data.neo4j.repository.query.CypherdslConditionExecutorImpl;
import org.springframework.data.neo4j.repository.query.QuerydslNeo4jPredicateExecutor;
import org.springframework.data.neo4j.repository.query.SimpleQueryByExampleExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;

/**
 * Unit tests for {@link Neo4jRepositoryFragmentsContributor}.
 *
 * @author Mark Paluch
 */
class Neo4jRepositoryFragmentsContributorUnitTests {

	Neo4jMappingContext mappingContext = new Neo4jMappingContext();
	Neo4jOperations operations = mock(Neo4jOperations.class);

	@Test
	void builtInContributorShouldCreateFragments() {

		RepositoryComposition.RepositoryFragments fragments = Neo4jRepositoryFragmentsContributor.DEFAULT.contribute(
				AbstractRepositoryMetadata.getMetadata(CypherdslRepository.class),
				new DefaultNeo4jEntityInformation<>(mappingContext.getPersistentEntity(ThingWithAllAdditionalTypes.class)),
				operations, mappingContext);

		assertThat(fragments).hasSize(2);

		Iterator<RepositoryFragment<?>> iterator = fragments.iterator();

		RepositoryFragment<?> queryByExample = iterator.next();
		assertThat(queryByExample.getImplementationClass()).contains(SimpleQueryByExampleExecutor.class);

		RepositoryFragment<?> cypherdsl = iterator.next();
		assertThat(cypherdsl.getImplementationClass()).contains(CypherdslConditionExecutorImpl.class);
	}

	@Test
	void builtInContributorShouldDescribeFragments() {

		RepositoryComposition.RepositoryFragments fragments = Neo4jRepositoryFragmentsContributor.DEFAULT
				.describe(AbstractRepositoryMetadata.getMetadata(ComposedRepository.class));

		assertThat(fragments).hasSize(3);

		Iterator<RepositoryFragment<?>> iterator = fragments.iterator();

		RepositoryFragment<?> queryByExample = iterator.next();
		assertThat(queryByExample.getImplementationClass()).contains(SimpleQueryByExampleExecutor.class);

		RepositoryFragment<?> querydsl = iterator.next();
		assertThat(querydsl.getImplementationClass()).contains(QuerydslNeo4jPredicateExecutor.class);

		RepositoryFragment<?> cypherdsl = iterator.next();
		assertThat(cypherdsl.getImplementationClass()).contains(CypherdslConditionExecutorImpl.class);
	}

	@Test
	void composedContributorShouldCreateFragments() {

		Neo4jRepositoryFragmentsContributor contributor = Neo4jRepositoryFragmentsContributor.DEFAULT
				.andThen(MyNeo4jRepositoryFragmentsContributor.INSTANCE);

		RepositoryComposition.RepositoryFragments fragments = contributor.contribute(
				AbstractRepositoryMetadata.getMetadata(QuerydslRepository.class),
				new DefaultNeo4jEntityInformation<>(mappingContext.getPersistentEntity(ThingWithAllAdditionalTypes.class)),
				operations, mappingContext);

		assertThat(fragments).hasSize(3);

		Iterator<RepositoryFragment<?>> iterator = fragments.iterator();

		RepositoryFragment<?> queryByExample = iterator.next();
		assertThat(queryByExample.getImplementationClass()).contains(SimpleQueryByExampleExecutor.class);

		RepositoryFragment<?> querydsl = iterator.next();
		assertThat(querydsl.getImplementationClass()).contains(QuerydslNeo4jPredicateExecutor.class);

		RepositoryFragment<?> additional = iterator.next();
		assertThat(additional.getImplementationClass()).contains(MyFragment.class);
	}

	enum MyNeo4jRepositoryFragmentsContributor implements Neo4jRepositoryFragmentsContributor {

		INSTANCE;

		@Override
		public RepositoryComposition.RepositoryFragments contribute(RepositoryMetadata metadata,
				Neo4jEntityInformation<?, ?> entityInformation, Neo4jOperations operations,
				Neo4jMappingContext mappingContext) {
			return RepositoryComposition.RepositoryFragments.just(new MyFragment());
		}

		@Override
		public RepositoryComposition.RepositoryFragments describe(RepositoryMetadata metadata) {
			return RepositoryComposition.RepositoryFragments.just(new MyFragment());
		}
	}

	static class MyFragment {

	}

	interface QuerydslRepository
			extends Repository<ThingWithAllAdditionalTypes, Long>, QuerydslPredicateExecutor<ThingWithAllAdditionalTypes> {}

	interface CypherdslRepository
			extends Repository<ThingWithAllAdditionalTypes, Long>, CypherdslConditionExecutor<ThingWithAllAdditionalTypes> {}

	interface ComposedRepository extends Repository<ThingWithAllAdditionalTypes, Long>,
			QuerydslPredicateExecutor<ThingWithAllAdditionalTypes>, CypherdslConditionExecutor<ThingWithAllAdditionalTypes> {}

}
