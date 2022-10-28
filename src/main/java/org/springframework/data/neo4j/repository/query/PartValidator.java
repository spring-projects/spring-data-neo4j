/*
 * Copyright 2011-2022 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.neo4j.driver.types.Point;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Support class for validating parts of either a {@link PartTreeNeo4jQuery} or the {@link ReactivePartTreeNeo4jQuery
 * reactive pendant}.
 *
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Abwasser
 * @since 6.0
 */
class PartValidator {

	/**
	 * A set of the temporal types that are directly passable to the driver and support a meaningful comparison in a
	 * temporal sense (after, before). See
	 * <a href="See https://neo4j.com/docs/driver-manual/1.7/cypher-values/#driver-neo4j-type-system" />
	 */
	private static final Set<Class<?>> COMPARABLE_TEMPORAL_TYPES;
	static {
		Set<Class<?>> hlp = new TreeSet<>(Comparator.comparing(Class::getName));
		hlp.addAll(Arrays.asList(LocalDate.class, OffsetTime.class, ZonedDateTime.class, LocalDateTime.class, Instant.class));
		COMPARABLE_TEMPORAL_TYPES = Collections.unmodifiableSet(hlp);
	}

	private static final EnumSet<Part.Type> TYPES_SUPPORTING_CASE_INSENSITIVITY = EnumSet.of(Part.Type.CONTAINING,
			Part.Type.ENDING_WITH, Part.Type.LIKE, Part.Type.NEGATING_SIMPLE_PROPERTY, Part.Type.NOT_CONTAINING,
			Part.Type.NOT_LIKE, Part.Type.SIMPLE_PROPERTY, Part.Type.STARTING_WITH);

	private static final EnumSet<Part.Type> TYPES_SUPPORTED_FOR_COMPOSITES = EnumSet.of(Part.Type.SIMPLE_PROPERTY, Part.Type.NEGATING_SIMPLE_PROPERTY);

	private final Neo4jMappingContext mappingContext;
	private final Neo4jQueryMethod queryMethod;

	PartValidator(Neo4jMappingContext mappingContext, Neo4jQueryMethod queryMethod) {
		this.mappingContext = mappingContext;
		this.queryMethod = queryMethod;
	}

	void validatePart(Part part) {

		validateIgnoreCase(part);
		switch (part.getType()) {
			case AFTER, BEFORE -> validateTemporalProperty(part);
			case IS_EMPTY, IS_NOT_EMPTY -> validateCollectionProperty(part);
			case NEAR, WITHIN -> validatePointProperty(part);
		}

		if (!TYPES_SUPPORTED_FOR_COMPOSITES.contains(part.getType())) {
			validateNotACompositeProperty(part);
		}
	}

	private void validateNotACompositeProperty(Part part) {

		PersistentPropertyPath<Neo4jPersistentProperty> path = mappingContext
				.getPersistentPropertyPath(part.getProperty());
		Neo4jPersistentProperty property = path.getRequiredLeafProperty();
		Assert.isTrue(!property.isComposite(), "Can not derive query for '%s': Derived queries are not supported for composite properties");
	}

	private void validateIgnoreCase(Part part) {

		Assert.isTrue(part.shouldIgnoreCase() != Part.IgnoreCaseType.ALWAYS || canIgnoreCase(part), () -> String.format(
				"Can not derive query for '%s': Only the case of String based properties can be ignored within the following keywords: %s",
				queryMethod, formatTypes(TYPES_SUPPORTING_CASE_INSENSITIVITY)));
	}

	private void validateTemporalProperty(Part part) {

		Assert.isTrue(COMPARABLE_TEMPORAL_TYPES.contains(part.getProperty().getLeafType()), () -> String.format(
				"Can not derive query for '%s': The keywords %s work only with properties with one of the following types: %s",
				queryMethod, formatTypes(Collections.singletonList(part.getType())), COMPARABLE_TEMPORAL_TYPES));
	}

	private void validateCollectionProperty(Part part) {

		Assert.isTrue(part.getProperty().getLeafProperty().isCollection(),
				() -> String.format("Can not derive query for '%s': The keywords %s work only with collection properties",
						queryMethod, formatTypes(Collections.singletonList(part.getType()))));
	}

	private void validatePointProperty(Part part) {

		Assert.isTrue(
				TypeInformation.of(Point.class)
						.isAssignableFrom(part.getProperty().getLeafProperty().getTypeInformation()),
				() -> String.format("Can not derive query for '%s': %s works only with spatial properties", queryMethod,
						part.getType()));
	}

	private static String formatTypes(Collection<Part.Type> types) {
		return types.stream().flatMap(t -> t.getKeywords().stream()).collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * Checks whether the given part can be queried without case sensitivity.
	 *
	 * @param part query part to check if ignoring case sensitivity is possible
	 * @return True when {@code part} can be queried case-insensitive.
	 */
	static boolean canIgnoreCase(Part part) {
		return part.getProperty().getLeafType() == String.class
				&& TYPES_SUPPORTING_CASE_INSENSITIVITY.contains(part.getType());
	}
}
