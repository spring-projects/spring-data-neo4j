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

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filters;
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

	static UnstartedBuild forType(Class<?> entityType) {
		return new UnstartedBuild(entityType);
	}

	private FilterBuildersDefinition(Class<?> entityType, Part basePart) {
		this.entityType = entityType;
		this.basePart = basePart;
		this.filterBuilders = new LinkedList<>();

		this.filterBuilders.add(FilterBuilder.forPartAndEntity(basePart, entityType, BooleanOperator.NONE));
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
		this.filterBuilders.add(FilterBuilder.forPartAndEntity(part, entityType, BooleanOperator.AND));
		return this;
	}

	FilterBuildersDefinition or(Part part) {
		this.filterBuilders.add(FilterBuilder.forPartAndEntity(part, entityType, BooleanOperator.OR));
		return this;
	}

	static class UnstartedBuild {
		private final Class<?> entityType;

		UnstartedBuild(Class<?> entityType) {
			this.entityType = entityType;
		}

		FilterBuildersDefinition startWith(Part firstPart) {
			return new FilterBuildersDefinition(entityType, firstPart);
		}
	}
}
