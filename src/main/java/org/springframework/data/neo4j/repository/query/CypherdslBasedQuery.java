/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingAndReturn;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.util.Assert;

/**
 * A repository query based on the Cypher-DSL. This variant has been introduced as it
 * turns out to be rather hard to access facts about the returned type or the returned
 * projection (if any).
 *
 * @author Michael J. Simons
 * @since 6.1
 */
final class CypherdslBasedQuery extends AbstractNeo4jQuery {

	private final Function<Statement, String> renderer;

	private CypherdslBasedQuery(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, Neo4jQueryType queryType, ProjectionFactory projectionFactory,
			Function<Statement, String> renderer) {
		super(neo4jOperations, mappingContext, queryMethod, queryType, projectionFactory);
		this.renderer = renderer;
	}

	static CypherdslBasedQuery create(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, ProjectionFactory projectionFactory, Function<Statement, String> renderer) {

		return new CypherdslBasedQuery(neo4jOperations, mappingContext, queryMethod, Neo4jQueryType.DEFAULT,
				projectionFactory, renderer);
	}

	@Override
	protected <T> PreparedQuery<T> prepareQuery(Class<T> returnedType,
			Collection<PropertyFilter.ProjectedPath> includedProperties, Neo4jParameterAccessor parameterAccessor,
			@Nullable Neo4jQueryType queryType,
			@Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction,
			UnaryOperator<Integer> limitModifier) {

		Object[] parameters = parameterAccessor.getValues();

		Assert.notEmpty(parameters, "Cypher based query methods must provide at least a statement parameter");
		Statement statement;
		if (this.queryMethod.isPageQuery()) {
			Assert.isInstanceOf(OngoingReadingAndReturn.class, parameters[0],
					"The first parameter to a Cypher based method must be an ongoing reading with a defined return clause");
			Assert.isInstanceOf(Statement.class, parameters[1],
					"The second parameter to a Cypher based method must be a statement");
			Assert.isInstanceOf(Pageable.class, parameters[2],
					"The third parameter to a Cypher based method must be a page request");
			Pageable pageable = (Pageable) parameters[2];
			statement = ((OngoingReadingAndReturn) parameters[0])
				.orderBy(CypherAdapterUtils.toSortItems(
						this.mappingContext.getNodeDescription(getDomainType(this.queryMethod)), pageable.getSort()))
				.skip(pageable.getOffset())
				.limit(limitModifier.apply(pageable.getPageSize()))
				.build();
		}
		else {
			Assert.isInstanceOf(Statement.class, parameters[0],
					"The first parameter to a Cypher based method must be a statement");
			statement = (Statement) parameters[0];
		}

		Map<String, Object> boundParameters = statement.getCatalog().getParameters();
		return PreparedQuery.queryFor(returnedType)
			.withCypherQuery(this.renderer.apply(statement))
			.withParameters(boundParameters)
			.usingMappingFunction(mappingFunction)
			.build();
	}

	@Override
	protected Optional<PreparedQuery<Long>> getCountQuery(Neo4jParameterAccessor parameterAccessor) {

		// We verified this above
		Statement countStatement = (Statement) parameterAccessor.getValues()[1];
		return Optional.of(PreparedQuery.queryFor(Long.class)
			.withCypherQuery(this.renderer.apply(countStatement))
			.withParameters(countStatement.getCatalog().getParameters())
			.build());
	}

}
