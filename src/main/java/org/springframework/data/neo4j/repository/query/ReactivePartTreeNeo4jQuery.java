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
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link RepositoryQuery} for derived finder methods.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class ReactivePartTreeNeo4jQuery extends AbstractReactiveNeo4jQuery {

	private final PartTree tree;

	public static RepositoryQuery create(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, ProjectionFactory factory) {
		return new ReactivePartTreeNeo4jQuery(neo4jOperations, mappingContext, queryMethod,
				new PartTree(queryMethod.getName(), getDomainType(queryMethod)), factory);
	}

	private ReactivePartTreeNeo4jQuery(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, PartTree tree, ProjectionFactory factory) {
		super(neo4jOperations, mappingContext, queryMethod, Neo4jQueryType.fromPartTree(tree), factory);

		this.tree = tree;
		// Validate parts. Sort properties will be validated by Spring Data already.
		PartValidator validator = new PartValidator(mappingContext, queryMethod);
		this.tree.flatMap(OrPart::stream).forEach(validator::validatePart);
	}

	@Override
	protected <T extends Object> PreparedQuery<T> prepareQuery(Class<T> returnedType, Map<PropertyPath, Boolean> includedProperties,
			Neo4jParameterAccessor parameterAccessor, @Nullable Neo4jQueryType queryType,
			@Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction) {

		CypherQueryCreator queryCreator = new CypherQueryCreator(mappingContext, getDomainType(queryMethod),
				Optional.ofNullable(queryType).orElseGet(() -> Neo4jQueryType.fromPartTree(tree)), tree, parameterAccessor,
				includedProperties, this::convertParameter, UnaryOperator.identity());

		QueryFragmentsAndParameters queryAndParameters = queryCreator.createQuery();

		return PreparedQuery.queryFor(returnedType).withQueryFragmentsAndParameters(queryAndParameters)
				.usingMappingFunction(mappingFunction).build();
	}
}
