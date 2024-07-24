/*
 * Copyright 2011-2024 the original author or authors.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.lang.Nullable;

/**
 * Supporting class containing some state and convenience methods for building fluent queries (both imperative and reactive).
 *
 * @author Michael J. Simons
 * @param <R> The result type
 * @soundtrack Die Ärzte - Geräusch
 */
abstract class FluentQuerySupport<R> {

	protected final Class<R> resultType;

	protected final Sort sort;

	protected final Integer limit;

	@Nullable
	protected final Set<String> properties;

	FluentQuerySupport(
			Class<R> resultType,
			Sort sort,
			@Nullable Integer limit,
			@Nullable Collection<String> properties
	) {
		this.resultType = resultType;
		this.sort = sort;
		this.limit = limit;
		if (properties != null) {
			this.properties = new HashSet<>(properties);
		} else {
			this.properties = null;
		}
	}

	final Predicate<PropertyFilter.RelaxedPropertyPath> createIncludedFieldsPredicate() {

		if (this.properties == null || this.properties.isEmpty()) {
			return PropertyFilter.NO_FILTER;
		}
		return path -> this.properties.contains(path.toDotPath());
	}

	final Collection<String> mergeProperties(Collection<String> additionalProperties) {
		Set<String> newProperties = new HashSet<>();
		if (this.properties != null) {
			newProperties.addAll(this.properties);
		}
		newProperties.addAll(additionalProperties);
		return Collections.unmodifiableCollection(newProperties);
	}

	final Window<R> scroll(ScrollPosition scrollPosition, List<R> rawResult, Neo4jPersistentEntity<?> entity) {

		var skip = scrollPosition.isInitial()
				? 0
				: (scrollPosition instanceof OffsetScrollPosition offsetScrollPosition) ? offsetScrollPosition.getOffset()
				: 0;

		var scrollDirection = scrollPosition instanceof KeysetScrollPosition keysetScrollPosition ? keysetScrollPosition.getDirection() : ScrollPosition.Direction.FORWARD;
		if (scrollDirection == ScrollPosition.Direction.BACKWARD) {
			Collections.reverse(rawResult);
		}

		IntFunction<? extends ScrollPosition> positionFunction = null;

		if (scrollPosition instanceof OffsetScrollPosition) {
			positionFunction = OffsetScrollPosition.positionFunction(skip);
		} else {
			positionFunction = v -> {
				var accessor = entity.getPropertyAccessor(rawResult.get(v));
				var keys = new LinkedHashMap<String, Object>();
				sort.forEach(o -> {
					// Storing the graph property name here
					var persistentProperty = entity.getRequiredPersistentProperty(o.getProperty());
					keys.put(persistentProperty.getPropertyName(), accessor.getProperty(persistentProperty));
				});
				keys.put(Constants.NAME_OF_ADDITIONAL_SORT, accessor.getProperty(entity.getRequiredIdProperty()));
				return ScrollPosition.forward(keys);
			};
		}
		return Window.from(getSubList(rawResult, limit, scrollDirection), positionFunction, hasMoreElements(rawResult, limit));
	}

	final Collection<String> extractAllPaths(Collection<String> projectingProperties) {
		if (projectingProperties.isEmpty()) {
			return new HashSet<>();
		}

		Set<String> allPaths = new HashSet<>();
		for (String property : projectingProperties) {
			if (property.contains(".")) {
				allPaths.add(property.substring(0, property.lastIndexOf(".")));
			}
			allPaths.add(property);
		}
		return allPaths;
	}

	private static boolean hasMoreElements(List<?> result, @Nullable Integer limit) {
		return !result.isEmpty() && result.size() > (limit != null ? limit : 0);
	}

	private static <T> List<T> getSubList(List<T> result, @Nullable Integer limit, ScrollPosition.Direction scrollDirection) {

		if (limit != null && limit > 0 && result.size() > limit) {
			return scrollDirection == ScrollPosition.Direction.FORWARD ? result.subList(0, limit) : result.subList(1, limit + 1);
		}

		return result;
	}
}
