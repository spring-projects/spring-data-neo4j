/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.repository.query;

import lombok.RequiredArgsConstructor;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.repository.support.ExecutableQuery;
import org.springframework.data.neo4j.repository.support.PreparedQuery;

/**
 * Set of classes to contain query execution strategies. Depending (mostly) on the return type of a
 * {@link org.springframework.data.repository.query.QueryMethod} a {@link AbstractNeo4jQuery} can be executed in various
 * flavors.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@FunctionalInterface
interface Neo4jQueryExecution {

	Object execute(PreparedQuery description, boolean asCollectionQuery);

	@RequiredArgsConstructor
	class DefaultQueryExecution implements Neo4jQueryExecution {

		private final Neo4jClient neo4jClient;

		@Override
		public Object execute(PreparedQuery description, boolean asCollectionQuery) {

			ExecutableQuery executableQuery = ExecutableQuery.create(description, neo4jClient);
			if (asCollectionQuery) {
				return executableQuery.getResults();
			} else {
				return executableQuery.getSingleResult();
			}
		}
	}
}
