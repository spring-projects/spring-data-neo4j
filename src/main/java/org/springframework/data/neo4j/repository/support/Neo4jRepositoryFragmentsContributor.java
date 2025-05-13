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

import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragmentsContributor;
import org.springframework.util.Assert;

/**
 * Neo4j-specific {@link RepositoryFragmentsContributor} contributing fragments based on the repository. Typically,
 * contributes Query by Example Executor, Querydsl, and Cypher condition DSL fragments.
 * <p>
 * Implementations must define a no-args constructor.
 *
 * @author Mark Paluch
 * @since 8.0
 * @see org.springframework.data.repository.query.QueryByExampleExecutor
 * @see org.springframework.data.querydsl.QuerydslPredicateExecutor
 * @see CypherdslConditionExecutor
 */
public interface Neo4jRepositoryFragmentsContributor extends RepositoryFragmentsContributor {

	Neo4jRepositoryFragmentsContributor DEFAULT = BuiltinContributor.INSTANCE;

	/**
	 * Returns a composed {@code Neo4jRepositoryFragmentsContributor} that first applies this contributor to its inputs,
	 * and then applies the {@code after} contributor concatenating effectively both results. If evaluation of either
	 * contributors throws an exception, it is relayed to the caller of the composed contributor.
	 *
	 * @param after the contributor to apply after this contributor is applied.
	 * @return a composed contributor that first applies this contributor and then applies the {@code after} contributor.
	 */
	default Neo4jRepositoryFragmentsContributor andThen(Neo4jRepositoryFragmentsContributor after) {

		Assert.notNull(after, "Neo4jRepositoryFragmentsContributor must not be null");

		return new Neo4jRepositoryFragmentsContributor() {

			@Override
			public RepositoryComposition.RepositoryFragments contribute(RepositoryMetadata metadata,
					Neo4jEntityInformation<?, ?> entityInformation, Neo4jOperations operations,
					Neo4jMappingContext mappingContext) {
				return Neo4jRepositoryFragmentsContributor.this
						.contribute(metadata, entityInformation, operations, mappingContext)
						.append(after.contribute(metadata, entityInformation, operations, mappingContext));
			}

			@Override
			public RepositoryComposition.RepositoryFragments describe(RepositoryMetadata metadata) {
				return Neo4jRepositoryFragmentsContributor.this.describe(metadata).append(after.describe(metadata));
			}
		};
	}

	/**
	 * Creates {@link RepositoryComposition.RepositoryFragments} based on {@link RepositoryMetadata} to add Neo4j-specific
	 * extensions.
	 *
	 * @param metadata repository metadata.
	 * @param entityInformation must not be {@literal null}.
	 * @param operations must not be {@literal null}.
	 * @param mappingContext must not be {@literal null}.
	 * @return {@link RepositoryComposition.RepositoryFragments} to be added to the repository.
	 */
	RepositoryComposition.RepositoryFragments contribute(RepositoryMetadata metadata,
			Neo4jEntityInformation<?, ?> entityInformation, Neo4jOperations operations, Neo4jMappingContext mappingContext);
}
