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

import static java.util.stream.Collectors.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.driver.types.Point;
import org.neo4j.springframework.data.core.Neo4jOperations;
import org.neo4j.springframework.data.core.PreparedQuery;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.Assert;

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

	PartTreeNeo4jQuery(
		Neo4jOperations neo4jOperations,
		Neo4jMappingContext mappingContext,
		Neo4jQueryMethod queryMethod
	) {
		super(neo4jOperations, mappingContext, queryMethod);

		this.tree = new PartTree(queryMethod.getName(), domainType);

		// Validate parts. Sort properties will be validated by Spring Data already.
		PartValidator validator = new PartValidator(queryMethod);
		this.tree.flatMap(OrPart::stream).forEach(validator::validatePart);
	}

	@Override
	protected PreparedQuery prepareQuery(ResultProcessor resultProcessor, Neo4jParameterAccessor parameterAccessor) {

		CypherQueryCreator queryCreator = new CypherQueryCreator(
			mappingContext, domainType, tree, parameterAccessor, getInputProperties(resultProcessor)
		);

		String cypherQuery = queryCreator.createQuery();
		Map<String, Object> boundedParameters = parameterAccessor.getParameters()
			.getBindableParameters().stream()
			.collect(toMap(Neo4jQueryMethod.Neo4jParameter::getNameOrIndex,
				formalParameter -> convertParameter(parameterAccessor.getBindableValue(formalParameter.getIndex()))));

		return PreparedQuery.queryFor(resultProcessor.getReturnedType().getReturnedType())
			.withCypherQuery(cypherQuery)
			.withParameters(boundedParameters)
			.usingMappingFunction(getMappingFunction(resultProcessor))
			.build();
	}

	/**
	 * Checks whether the given part can be queried without case sensitivity.
	 *
	 * @param part query part to check if ignoring case sensitivity is possible
	 * @return True when {@code part} can be queried case insensitive.
	 */
	static boolean canIgnoreCase(Part part) {
		return part.getProperty().getLeafType() == String.class && TYPES_SUPPORTING_CASE_INSENSITIVITY
			.contains(part.getType());
	}

	@Override
	protected boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	protected boolean isExistsQuery() {
		return tree.isExistsProjection();
	}

	@Override
	protected boolean isDeleteQuery() {
		return tree.isDelete();
	}

	@Override
	protected boolean isLimiting() {
		return tree.isLimiting();
	}

	static class PartValidator {

		private final Neo4jQueryMethod queryMethod;

		PartValidator(Neo4jQueryMethod queryMethod) {
			this.queryMethod = queryMethod;
		}

		void validatePart(Part part) {

			validateIgnoreCase(part);
			switch (part.getType()) {
				case AFTER:
				case BEFORE:
					validateTemporalProperty(part);
					break;
				case IS_EMPTY:
				case IS_NOT_EMPTY:
					validateCollectionProperty(part);
					break;
				case NEAR:
				case WITHIN:
					validatePointProperty(part);
					break;
			}
		}

		private void validateIgnoreCase(Part part) {

			Assert.isTrue(part.shouldIgnoreCase() != Part.IgnoreCaseType.ALWAYS || canIgnoreCase(part),
				() -> String.format(
					"Can not derive query for '%s': Only the case of String based properties can be ignored within the following keywords: %s",
					queryMethod,
					formatTypes(TYPES_SUPPORTING_CASE_INSENSITIVITY)));
		}

		private void validateTemporalProperty(Part part) {

			Assert.isTrue(COMPARABLE_TEMPORAL_TYPES.contains(part.getProperty().getLeafType()), () -> String
				.format(
					"Can not derive query for '%s': The keywords %s work only with properties with one of the following types: %s",
					queryMethod, formatTypes(Collections.singletonList(part.getType())),
					COMPARABLE_TEMPORAL_TYPES));
		}

		private void validateCollectionProperty(Part part) {

			Assert.isTrue(part.getProperty().getLeafProperty().isCollection(), () -> String
				.format("Can not derive query for '%s': The keywords %s work only with collection properties",
					queryMethod,
					formatTypes(Collections.singletonList(part.getType()))));
		}

		private void validatePointProperty(Part part) {

			Assert.isTrue(ClassTypeInformation.from(Point.class)
				.isAssignableFrom(part.getProperty().getLeafProperty().getTypeInformation()), () -> String
				.format("Can not derive query for '%s': %s works only with spatial properties", queryMethod,
					part.getType()));
		}

		private static String formatTypes(Collection<Part.Type> types) {
			return types.stream().flatMap(t -> t.getKeywords().stream()).collect(joining(", ", "[", "]"));
		}
	}
}
