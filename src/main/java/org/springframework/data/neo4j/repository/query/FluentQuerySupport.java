/*
 * Copyright 2011-2023 the original author or authors.
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
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.data.domain.Sort;
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

	@Nullable
	protected final Set<String> properties;

	FluentQuerySupport(
			Class<R> resultType,
			Sort sort,
			@Nullable Collection<String> properties
	) {
		this.resultType = resultType;
		this.sort = sort;
		if (properties != null) {
			this.properties = new HashSet<>(properties);
		} else {
			this.properties = null;
		}
	}

	final Predicate<PropertyFilter.RelaxedPropertyPath> createIncludedFieldsPredicate() {

		if (this.properties == null) {
			return path -> true;
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
}
