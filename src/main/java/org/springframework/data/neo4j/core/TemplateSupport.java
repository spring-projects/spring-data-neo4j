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
package org.springframework.data.neo4j.core;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.springframework.lang.Nullable;

/**
 * Utilities for templates.
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since 6.1
 */
@API(status = API.Status.INTERNAL, since = "6.1")
final class TemplateSupport {

	enum FetchType {

		ONE,
		ALL
	}

	@Nullable
	static Class<?> findCommonElementType(Iterable<?> collection) {
		Class<?> candidate = null;
		for (Object val : collection) {
			if (val != null) {
				if (candidate == null) {
					candidate = val.getClass();
				} else if (candidate != val.getClass()) {
					return null;
				}
			}
		}
		return candidate;
	}

	static Predicate<String> computeIncludePropertyPredicate(List<PropertyDescriptor> includedProperties) {

		if (includedProperties == null) {
			return p -> true;
		} else {
			Set<String> includedPropertyNames = includedProperties.stream().map(PropertyDescriptor::getName).collect(
					Collectors.toSet());
			return includedPropertyNames::contains;
		}
	}

	private TemplateSupport() {
	}
}
