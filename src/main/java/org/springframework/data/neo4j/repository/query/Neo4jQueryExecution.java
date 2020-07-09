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
package org.springframework.data.neo4j.repository.query;

import reactor.core.publisher.Mono;

import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;

/**
 * Set of classes to contain query execution strategies. Depending (mostly) on the return type of a
 * {@link org.springframework.data.repository.query.QueryMethod} a {@link AbstractNeo4jQuery} can be executed in various
 * flavors.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@FunctionalInterface
interface Neo4jQueryExecution {

	Object execute(PreparedQuery description, boolean asCollectionQuery);

	class DefaultQueryExecution implements Neo4jQueryExecution {

		private final Neo4jOperations neo4jOperations;

		DefaultQueryExecution(Neo4jOperations neo4jOperations) {
			this.neo4jOperations = neo4jOperations;
		}

		@Override
		public Object execute(PreparedQuery preparedQuery, boolean asCollectionQuery) {

			Neo4jOperations.ExecutableQuery executableQuery = neo4jOperations.toExecutableQuery(preparedQuery);
			if (asCollectionQuery) {
				return executableQuery.getResults();
			} else {
				return executableQuery.getSingleResult();
			}
		}
	}

	class ReactiveQueryExecution implements Neo4jQueryExecution {

		private final ReactiveNeo4jOperations neo4jOperations;

		ReactiveQueryExecution(ReactiveNeo4jOperations neo4jOperations) {
			this.neo4jOperations = neo4jOperations;
		}

		@Override
		public Object execute(PreparedQuery preparedQuery, boolean asCollectionQuery) {

			Mono<ReactiveNeo4jOperations.ExecutableQuery> executableQuery = neo4jOperations.toExecutableQuery(preparedQuery);
			if (asCollectionQuery) {
				return executableQuery.flatMapMany(q -> q.getResults());
			} else {
				return executableQuery.flatMap(q -> q.getSingleResult());
			}
		}
	}
}
