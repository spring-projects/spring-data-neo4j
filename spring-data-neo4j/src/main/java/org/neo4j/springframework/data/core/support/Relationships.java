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
package org.neo4j.springframework.data.core.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;

/**
 * @author Michael J. Simons
 */
public final class Relationships {

	/**
	 * The value for a relationship can be a scalar object (1:1), a collection (1:n) or a map (1:n, but with dynamic
	 * relationship types). This method unifies the type into something iterable, depending on the given inverse type.
	 *
	 * @param rawValue The raw value to unify
	 * @return A unified collection (Either a collection of Map.Entry or a list of related values)
	 */
	public static Collection<?> unifyRelationshipValue(Neo4jPersistentProperty property, Object rawValue) {
		Collection<?> unifiedValue;
		if (property.isDynamicAssociation()) {
			unifiedValue = ((Map<String, Object>) rawValue).entrySet();
		} else if (property.isCollectionLike()) {
			unifiedValue = (Collection<Object>) rawValue;
		} else {
			unifiedValue = Collections.singleton(rawValue);
		}
		return unifiedValue;
	}

	private Relationships() {
	}
}
