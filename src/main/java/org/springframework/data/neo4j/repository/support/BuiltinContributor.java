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

import static org.springframework.data.querydsl.QuerydslUtils.*;

import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.query.CypherdslConditionExecutorImpl;
import org.springframework.data.neo4j.repository.query.QuerydslNeo4jPredicateExecutor;
import org.springframework.data.neo4j.repository.query.SimpleQueryByExampleExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragmentsContributor;

/**
 * Built-in {@link RepositoryFragmentsContributor} contributing Query by Example, Querydsl, and Cypher condition
 * fragments if a repository implements the corresponding interfaces.
 *
 * @author Mark Paluch
 * @since 8.0
 */
enum BuiltinContributor implements Neo4jRepositoryFragmentsContributor {

	INSTANCE;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public RepositoryFragments contribute(RepositoryMetadata metadata, Neo4jEntityInformation<?, ?> entityInformation,
			Neo4jOperations operations, Neo4jMappingContext mappingContext) {

		RepositoryFragments fragments = RepositoryFragments
				.of(RepositoryFragment.implemented(new SimpleQueryByExampleExecutor(operations, mappingContext)));

		if (isQuerydslRepository(metadata)) {
			fragments = fragments.append(RepositoryFragment
					.implemented(new QuerydslNeo4jPredicateExecutor(mappingContext, entityInformation, operations)));
		}

		if (CypherdslConditionExecutor.class.isAssignableFrom(metadata.getRepositoryInterface())) {
			fragments = fragments
					.append(RepositoryFragment.implemented(new CypherdslConditionExecutorImpl(entityInformation, operations)));
		}

		return fragments;
	}

	@Override
	public RepositoryFragments describe(RepositoryMetadata metadata) {

		RepositoryFragments fragments = RepositoryFragments
				.of(RepositoryFragment.structural(SimpleQueryByExampleExecutor.class));

		if (isQuerydslRepository(metadata)) {
			fragments = fragments.append(RepositoryFragment.structural(QuerydslNeo4jPredicateExecutor.class));
		}

		if (CypherdslConditionExecutor.class.isAssignableFrom(metadata.getRepositoryInterface())) {
			fragments = fragments.append(RepositoryFragment.structural(CypherdslConditionExecutorImpl.class));
		}

		return fragments;
	}

	private static boolean isQuerydslRepository(RepositoryMetadata metadata) {
		return QUERY_DSL_PRESENT && QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());
	}
}
