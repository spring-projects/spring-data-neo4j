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
package org.neo4j.springframework.data.repository.query;

import org.neo4j.springframework.data.core.PreparedQuery;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryQuery} implementations for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
abstract class AbstractReactiveNeo4jQuery extends Neo4jQuerySupport implements RepositoryQuery {

	final ReactiveNeo4jClient neo4jClient;
	final Neo4jMappingContext mappingContext;
	final Neo4jQueryMethod queryMethod;
	final Class<?> domainType;

	AbstractReactiveNeo4jQuery(ReactiveNeo4jClient neo4jClient,
		Neo4jMappingContext mappingContext, Neo4jQueryMethod queryMethod) {

		Assert.notNull(neo4jClient, "The Neo4j client is required.");
		Assert.notNull(mappingContext, "The mapping context is required.");
		Assert.notNull(queryMethod, "Query method must not be null!");

		this.neo4jClient = neo4jClient;
		this.mappingContext = mappingContext;
		this.queryMethod = queryMethod;
		this.domainType = queryMethod.getReturnedObjectType();
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	@Override
	public final Object execute(Object[] parameters) {
		return new Neo4jQueryExecution.ReactiveQueryExecution(neo4jClient)
			.execute(prepareQuery(parameters), queryMethod.isCollectionQuery());
	}

	protected abstract PreparedQuery prepareQuery(Object[] parameters);

	/**
	 *
	 * @return True if the query shout get a count projection applied.
	 */
	protected abstract boolean isCountQuery();

	/**
	 * @return True if the query should get an exists projection applied.
	 */
	protected abstract boolean isExistsQuery();

	/**
	 * @return True if the query should delete matching nodes.
	 */
	protected abstract boolean isDeleteQuery();

	/**
	 * @return True if the query has an explicit limit set.
	 */
	protected abstract boolean isLimiting();
}
