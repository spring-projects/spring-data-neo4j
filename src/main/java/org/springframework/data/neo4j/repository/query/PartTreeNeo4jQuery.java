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
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
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
final class PartTreeNeo4jQuery extends AbstractNeo4jQuery {

	private final PartTree tree;
	private final List<CustomStatementKreator> kreatorBeans;

	public static RepositoryQuery create(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
										 Neo4jQueryMethod queryMethod, ProjectionFactory factory, List<CustomStatementKreator> kreatorBeans) {
		return new PartTreeNeo4jQuery(neo4jOperations, mappingContext, queryMethod,
				new PartTree(queryMethod.getName(), getDomainType(queryMethod)), factory, kreatorBeans);
	}

	private PartTreeNeo4jQuery(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, PartTree tree, ProjectionFactory factory, List<CustomStatementKreator> kreatorBeans) {
		super(neo4jOperations, mappingContext, queryMethod, Neo4jQueryType.fromPartTree(tree), factory);

		this.tree = tree;
		this.kreatorBeans = kreatorBeans;
		// Validate parts. Sort properties will be validated by Spring Data already.
		PartValidator validator = new PartValidator(mappingContext, queryMethod);
		this.tree.flatMap(OrPart::stream).forEach(validator::validatePart);
	}

	@Override
	protected <T extends Object> PreparedQuery<T> prepareQuery(Class<T> returnedType, Collection<PropertyFilter.ProjectedPath> includedProperties,
			Neo4jParameterAccessor parameterAccessor, @Nullable Neo4jQueryType queryType,
			@Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction, UnaryOperator<Integer> limitModifier) {

		CustomStatementKreator kreatorBean = kreatorBeans
				.stream()
				.filter(bean -> bean.supports(queryMethod.getRepositoryName(), queryMethod.getNamedQueryName()))
				.findFirst()
				.orElse(QueryFragments.KREATOR);

		CypherQueryCreator queryCreator = new CypherQueryCreator(mappingContext, queryMethod, getDomainType(queryMethod),
				Optional.ofNullable(queryType).orElseGet(() -> Neo4jQueryType.fromPartTree(tree)), tree, parameterAccessor,
				includedProperties, this::convertParameter, limitModifier, kreatorBean);

		QueryFragmentsAndParameters queryAndParameters = queryCreator.createQuery();
		return PreparedQuery.queryFor(returnedType).withQueryFragmentsAndParameters(queryAndParameters)
				.usingMappingFunction(mappingFunction).build();
	}
}
