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
package org.springframework.data.neo4j.repository.query.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Predicate;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.lang.Nullable;

/**
 * The name of this class is wrong: It builds a list of filters, not a single filter. Starting with Neo4j-OGM 4.0,
 * there will be an actual filter builder in OGM and this class needs to be renamed.
 *
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public abstract class FilterBuilder {

	protected Part part;
	protected BooleanOperator booleanOperator;
	protected Class<?> entityType;
	protected Predicate<Part> isInternalIdProperty = part -> false;

	public static FilterBuilder forPartAndEntity(Part part, Class<?> entityType, BooleanOperator booleanOperator,
			Predicate<Part> isInternalIdProperty) {
		FilterBuilder filterBuilder;
		switch (part.getType()) {
			case NEAR:
				filterBuilder = new DistanceComparisonBuilder(part, booleanOperator, entityType);
				break;
			case BETWEEN:
				filterBuilder = new BetweenComparisonBuilder(part, booleanOperator, entityType);
				break;
			case NOT_CONTAINING:
			case CONTAINING:
				filterBuilder = resolveMatchingContainsFilterBuilder(part, entityType, booleanOperator);
				break;
			case IS_NULL:
			case IS_NOT_NULL:
				filterBuilder = new IsNullFilterBuilder(part, booleanOperator, entityType);
				break;
			case EXISTS:
				filterBuilder = new ExistsFilterBuilder(part, booleanOperator, entityType);
				break;
			case TRUE:
			case FALSE:
				filterBuilder = new BooleanComparisonBuilder(part, booleanOperator, entityType);
				break;
			default:
				filterBuilder = new PropertyComparisonBuilder(part, booleanOperator, entityType);
				break;
		}

		filterBuilder.isInternalIdProperty = isInternalIdProperty;
		return filterBuilder;
	}

	FilterBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		this.part = part;
		this.booleanOperator = booleanOperator;
		this.entityType = entityType;
	}

	public abstract List<Filter> build(Stack<Object> params);

	boolean isNegated() {
		return part.getType().name().startsWith("NOT");
	}

	protected String propertyName() {
		return part.getProperty().getSegment();
	}

	protected final NestedAttributes getNestedAttributes(Part part) {

		PropertyPath property = part.getProperty();
		if (!property.hasNext()) {
			return EMPTY_NESTED_ATTRIBUTES;
		} else {
			List<Filter.NestedPathSegment> segments = new ArrayList<>();
			segments.add(new Filter.NestedPathSegment(property.getSegment(), property.getType()));
			segments.addAll(deepNestedProperty(property));
			return new NestedAttributes(property.getOwningType().getType(), segments,
					property.getLeafProperty().getSegment());
		}
	}

	public static final NestedAttributes EMPTY_NESTED_ATTRIBUTES = new NestedAttributes(Void.class,
			Collections.emptyList(), null);

	protected static final class NestedAttributes {

		private final Class<?> owningType;
		private final List<Filter.NestedPathSegment> segments;
		private final String leafPropertySegment;

		NestedAttributes(Class<?> owningType, List<Filter.NestedPathSegment> segments,
				@Nullable String leafPropertySegment) {
			this.owningType = owningType;
			this.segments = new ArrayList<>(segments);
			this.leafPropertySegment = leafPropertySegment;
		}

		public Filter.NestedPathSegment[] getSegments() {
			return segments.toArray(new Filter.NestedPathSegment[segments.size()]);
		}

		public String getLeafPropertySegment() {
			return leafPropertySegment;
		}

		public boolean isEmpty() {
			return this == EMPTY_NESTED_ATTRIBUTES;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			NestedAttributes that = (NestedAttributes) o;
			return owningType.equals(that.owningType) &&
					segments.equals(that.segments) &&
					Objects.equals(leafPropertySegment, that.leafPropertySegment);
		}

		@Override public int hashCode() {
			return Objects.hash(owningType, segments, leafPropertySegment);
		}
	}

	private List<Filter.NestedPathSegment> deepNestedProperty(PropertyPath path) {
		List<Filter.NestedPathSegment> segments = new ArrayList<>();
		if (path.hasNext()) {
			PropertyPath next = path.next();
			if (!next.equals(next.getLeafProperty())) {
				segments.add(new Filter.NestedPathSegment(next.getSegment(), next.getType()));
				segments.addAll(deepNestedProperty(next));
			}
		}
		return segments;
	}

	private static FilterBuilder resolveMatchingContainsFilterBuilder(Part part, Class<?> entityType,
			BooleanOperator booleanOperator) {
		boolean usePropertyComparison = !part.getProperty().getTypeInformation().isCollectionLike();
		if (usePropertyComparison) {
			return new PropertyComparisonBuilder(part, booleanOperator, entityType);
		}
		return new ContainsComparisonBuilder(part, booleanOperator, entityType);
	}
}
