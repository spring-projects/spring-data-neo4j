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

import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithAllAdditionalTypes;
import org.springframework.data.neo4j.repository.query.ReactiveCypherdslConditionExecutorImpl;
import org.springframework.data.neo4j.repository.query.ReactiveQuerydslNeo4jPredicateExecutor;
import org.springframework.data.neo4j.repository.query.SimpleReactiveQueryByExampleExecutor;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;

/**
 * Unit tests for {@link ReactiveNeo4jRepositoryFragmentsContributor}.
 *
 * @author Mark Paluch
 */
class ReactiveNeo4jRepositoryFragmentsContributorUnitTests {

	Neo4jMappingContext mappingContext = new Neo4jMappingContext();
	ReactiveNeo4jOperations operations = mock(ReactiveNeo4jOperations.class);

	@Test
	void builtInContributorShouldCreateFragments() {

		RepositoryComposition.RepositoryFragments fragments = ReactiveNeo4jRepositoryFragmentsContributor.DEFAULT
				.contribute(AbstractRepositoryMetadata.getMetadata(CypherdslRepository.class),
						new DefaultNeo4jEntityInformation<>(mappingContext.getPersistentEntity(ThingWithAllAdditionalTypes.class)),
						operations, mappingContext);

		assertThat(fragments).hasSize(2);

		Iterator<RepositoryFragment<?>> iterator = fragments.iterator();

		RepositoryFragment<?> queryByExample = iterator.next();
		assertThat(queryByExample.getImplementationClass()).contains(SimpleReactiveQueryByExampleExecutor.class);

		RepositoryFragment<?> cypherdsl = iterator.next();
		assertThat(cypherdsl.getImplementationClass()).contains(ReactiveCypherdslConditionExecutorImpl.class);
	}

	@Test
	void builtInContributorShouldDescribeFragments() {

		RepositoryComposition.RepositoryFragments fragments = ReactiveNeo4jRepositoryFragmentsContributor.DEFAULT
				.describe(AbstractRepositoryMetadata.getMetadata(ComposedRepository.class));

		assertThat(fragments).hasSize(3);

		Iterator<RepositoryFragment<?>> iterator = fragments.iterator();

		RepositoryFragment<?> queryByExample = iterator.next();
		assertThat(queryByExample.getImplementationClass()).contains(SimpleReactiveQueryByExampleExecutor.class);

		RepositoryFragment<?> querydsl = iterator.next();
		assertThat(querydsl.getImplementationClass()).contains(ReactiveQuerydslNeo4jPredicateExecutor.class);

		RepositoryFragment<?> cypherdsl = iterator.next();
		assertThat(cypherdsl.getImplementationClass()).contains(ReactiveCypherdslConditionExecutorImpl.class);
	}

	@Test
	void composedContributorShouldCreateFragments() {

		ReactiveNeo4jRepositoryFragmentsContributor contributor = ReactiveNeo4jRepositoryFragmentsContributor.DEFAULT
				.andThen(MyNeo4jRepositoryFragmentsContributor.INSTANCE);

		RepositoryComposition.RepositoryFragments fragments = contributor.contribute(
				AbstractRepositoryMetadata.getMetadata(QuerydslRepository.class),
				new DefaultNeo4jEntityInformation<>(mappingContext.getPersistentEntity(ThingWithAllAdditionalTypes.class)),
				operations, mappingContext);

		assertThat(fragments).hasSize(3);

		Iterator<RepositoryFragment<?>> iterator = fragments.iterator();

		RepositoryFragment<?> queryByExample = iterator.next();
		assertThat(queryByExample.getImplementationClass()).contains(SimpleReactiveQueryByExampleExecutor.class);

		RepositoryFragment<?> querydsl = iterator.next();
		assertThat(querydsl.getImplementationClass()).contains(ReactiveQuerydslNeo4jPredicateExecutor.class);

		RepositoryFragment<?> additional = iterator.next();
		assertThat(additional.getImplementationClass()).contains(MyFragment.class);
	}

	enum MyNeo4jRepositoryFragmentsContributor implements ReactiveNeo4jRepositoryFragmentsContributor {

		INSTANCE;

		@Override
		public RepositoryComposition.RepositoryFragments contribute(RepositoryMetadata metadata,
				Neo4jEntityInformation<?, ?> entityInformation, ReactiveNeo4jOperations operations,
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

	interface QuerydslRepository extends Repository<ThingWithAllAdditionalTypes, Long>,
			ReactiveQuerydslPredicateExecutor<ThingWithAllAdditionalTypes> {}

	interface CypherdslRepository extends Repository<ThingWithAllAdditionalTypes, Long>,
			ReactiveCypherdslConditionExecutor<ThingWithAllAdditionalTypes> {}

	interface ComposedRepository extends Repository<ThingWithAllAdditionalTypes, Long>,
			ReactiveQuerydslPredicateExecutor<ThingWithAllAdditionalTypes>,
			ReactiveCypherdslConditionExecutor<ThingWithAllAdditionalTypes> {}

}
