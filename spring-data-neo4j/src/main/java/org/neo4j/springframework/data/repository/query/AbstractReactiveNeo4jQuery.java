/*
 * Copyright (c) 2019-2020 "Neo4j,"
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

import java.util.List;
import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.PreparedQuery;
import org.neo4j.springframework.data.core.ReactiveNeo4jOperations;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryQuery} implementations for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
abstract class AbstractReactiveNeo4jQuery extends Neo4jQuerySupport implements RepositoryQuery {

	protected final ReactiveNeo4jOperations neo4jOperations;

	AbstractReactiveNeo4jQuery(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
		Neo4jQueryMethod queryMethod, Neo4jQueryType queryType) {

		super(mappingContext, queryMethod, queryType);

		Assert.notNull(neo4jOperations, "The Neo4j operations are required.");
		this.neo4jOperations = neo4jOperations;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	@Override
	public final Object execute(Object[] parameters) {

		Neo4jParameterAccessor parameterAccessor = getParameterAccessor(parameters);
		ResultProcessor resultProcessor = queryMethod.getResultProcessor().withDynamicProjection(parameterAccessor);

		PreparedQuery<?> preparedQuery = prepareQuery(resultProcessor.getReturnedType().getReturnedType(),
			getInputProperties(resultProcessor), parameterAccessor, null, getMappingFunction(resultProcessor));

		Object rawResult = new Neo4jQueryExecution.ReactiveQueryExecution(neo4jOperations).execute(
			preparedQuery, queryMethod.isCollectionLikeQuery());

		return resultProcessor.processResult(rawResult, OptionalUnwrappingConverter.INSTANCE);
	}

	protected abstract <T extends Object> PreparedQuery<T> prepareQuery(
		Class<T> returnedType, List<String> includedProperties, Neo4jParameterAccessor parameterAccessor,
		@Nullable Neo4jQueryType queryType,
		@Nullable BiFunction<TypeSystem, Record, ?> mappingFunction);
}
