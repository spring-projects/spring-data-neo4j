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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link RepositoryQuery} for derived finder methods.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
final class PartTreeNeo4jQuery extends AbstractNeo4jQuery {

	/**
	 * A set of the temporal types that are directly passable to the driver and support a meaningful comparision in a
	 * temporal sense (after, before).
	 * See <a href="See https://neo4j.com/docs/driver-manual/1.7/cypher-values/#driver-neo4j-type-system" />
	 */
	private static final Set<Class<?>> COMPARABLE_TEMPORAL_TYPES = Collections
		.unmodifiableSet(new HashSet<>(Arrays.asList(LocalDate.class, OffsetTime.class, ZonedDateTime.class,
			LocalDateTime.class, Instant.class)));

	private static final EnumSet<Part.Type> TYPES_SUPPORTING_CASE_INSENSITIVITY = EnumSet
		.of(Part.Type.CONTAINING, Part.Type.ENDING_WITH, Part.Type.LIKE, Part.Type.NEGATING_SIMPLE_PROPERTY,
			Part.Type.NOT_CONTAINING,
			Part.Type.NOT_LIKE, Part.Type.SIMPLE_PROPERTY, Part.Type.STARTING_WITH);

	private final PartTree tree;

	public static RepositoryQuery create(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
		Neo4jQueryMethod queryMethod) {
		return new PartTreeNeo4jQuery(neo4jOperations, mappingContext, queryMethod,
			new PartTree(queryMethod.getName(), queryMethod.getDomainClass()));
	}

	private PartTreeNeo4jQuery(
		Neo4jOperations neo4jOperations,
		Neo4jMappingContext mappingContext,
		Neo4jQueryMethod queryMethod,
		PartTree tree
	) {
		super(neo4jOperations, mappingContext, queryMethod, Neo4jQueryType.fromPartTree(tree));

		this.tree = tree;
		// Validate parts. Sort properties will be validated by Spring Data already.
		PartValidator validator = new PartValidator(queryMethod);
		this.tree.flatMap(OrPart::stream).forEach(validator::validatePart);
	}

	@Override
	protected <T extends Object> PreparedQuery<T> prepareQuery(
		Class<T> returnedType, List<String> includedProperties, Neo4jParameterAccessor parameterAccessor,
		@Nullable Neo4jQueryType queryType,
		@Nullable BiFunction<TypeSystem, Record, ?> mappingFunction) {

		CypherQueryCreator queryCreator = new CypherQueryCreator(
			mappingContext, domainType, Optional.ofNullable(queryType).orElseGet(() -> Neo4jQueryType.fromPartTree(tree)), tree, parameterAccessor,
			includedProperties,
			this::convertParameter
		);

		QueryAndParameters queryAndParameters = queryCreator.createQuery();

		return PreparedQuery.queryFor(returnedType)
			.withCypherQuery(queryAndParameters.getQuery())
			.withParameters(queryAndParameters.getParameters())
			.usingMappingFunction(mappingFunction)
			.build();
	}
}
