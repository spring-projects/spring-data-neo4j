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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filters;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.repository.query.filter.FilterBuilder;
import org.springframework.data.repository.query.parser.Part;

/**
 * A filter builder definition represents an ongoing state of {@link FilterBuilder filter builder} chained together with
 * "and" or "or". It is used with a resolved parameters to create an instance {@link Filters} for building a query
 * object.
 *
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
class FilterBuildersDefinition {

	private final Class<?> entityType;

	private final Part basePart;

	private final List<FilterBuilder> filterBuilders;
	private final Predicate<Part> isInternalIdProperty;

	static UnstartedBuild forType(Neo4jMappingContext mappingContext, Class<?> entityType) {
		return new UnstartedBuild(mappingContext, entityType);
	}

	private FilterBuildersDefinition(Neo4jMappingContext mappingContext, Class<?> entityType, Part basePart) {
		this.entityType = entityType;
		this.basePart = basePart;
		this.filterBuilders = new LinkedList<>();
		this.isInternalIdProperty = part -> {
			PersistentPropertyPath<Neo4jPersistentProperty> path = mappingContext
					.getPersistentPropertyPath(part.getProperty());
			Neo4jPersistentProperty possibleIdProperty = path.getRequiredLeafProperty();
			return possibleIdProperty.isInternalIdProperty();
		};
		this.filterBuilders.add(FilterBuilder.forPartAndEntity(basePart, entityType, BooleanOperator.NONE,
				isInternalIdProperty));
	}

	TemplatedQuery buildTemplatedQuery() {
		return new TemplatedQuery(Collections.unmodifiableList(filterBuilders));
	}

	/**
	 * Comment on original DerivedQueryDefinition was: "because the OR is handled in a weird way. Luanne, explain better"
	 *
	 * @return
	 */
	Part getBasePart() {
		return basePart;
	}

	FilterBuildersDefinition and(Part part) {
		this.filterBuilders.add(FilterBuilder.forPartAndEntity(part, entityType, BooleanOperator.AND, isInternalIdProperty));
		return this;
	}

	FilterBuildersDefinition or(Part part) {
		this.filterBuilders.add(FilterBuilder.forPartAndEntity(part, entityType, BooleanOperator.OR, isInternalIdProperty));
		return this;
	}

	static class UnstartedBuild {

		private final Neo4jMappingContext mappingContext;

		private final Class<?> entityType;

		UnstartedBuild(Neo4jMappingContext mappingContext, Class<?> entityType) {
			this.mappingContext = mappingContext;
			this.entityType = entityType;
		}

		FilterBuildersDefinition startWith(Part firstPart) {
			return new FilterBuildersDefinition(mappingContext, entityType, firstPart);
		}
	}
}
