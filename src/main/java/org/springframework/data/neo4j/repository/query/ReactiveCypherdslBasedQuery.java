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
package org.springframework.data.neo4j.repository.query;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.util.Assert;

/**
 * A repository query based on the Cypher-DSL. This variant has been introduced as it turns out to be rather hard to access
 * facts about the returned type or the returned projection (if any).
 *
 * @author Michael J. Simons
 * @since 6.1
 */
final class ReactiveCypherdslBasedQuery extends AbstractReactiveNeo4jQuery {

	static ReactiveCypherdslBasedQuery create(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, ProjectionFactory projectionFactory) {

		return new ReactiveCypherdslBasedQuery(neo4jOperations, mappingContext, queryMethod, Neo4jQueryType.DEFAULT, projectionFactory);
	}

	private ReactiveCypherdslBasedQuery(ReactiveNeo4jOperations neo4jOperations,
			Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, Neo4jQueryType queryType, ProjectionFactory projectionFactory) {
		super(neo4jOperations, mappingContext, queryMethod, queryType, projectionFactory);
	}

	@Override
	protected <T> PreparedQuery<T> prepareQuery(Class<T> returnedType, Map<PropertyPath, Boolean> includedProperties,
			Neo4jParameterAccessor parameterAccessor, Neo4jQueryType queryType,
			Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction) {

		Object[] parameters = parameterAccessor.getValues();

		Assert.notEmpty(parameters, "Cypher based query methods must provide at least a statement parameter.");
		Assert.isInstanceOf(Statement.class, parameters[0],
				"The first parameter to a Cypher based method must be a statement.");
		Statement statement = (Statement) parameters[0];

		Map<String, Object> boundParameters = statement.getParameters();
		return PreparedQuery.queryFor(returnedType)
				.withCypherQuery(statement.getCypher())
				.withParameters(boundParameters)
				.usingMappingFunction(mappingFunction)
				.build();
	}
}
