/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;

/**
 * Internal helper class that takes care of tracking whether a related object or a
 * collection of related objects was recreated due to changing immutable properties.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "6.1")
final class RelationshipHandler {

	private static final int DEFAULT_SIZE = 32;

	private final Neo4jPersistentProperty property;

	/**
	 * The raw value as passed to the template.
	 */
	@Nullable
	private final Object rawValue;

	private final Cardinality cardinality;

	private final Map<Object, Object> newRelatedObjectsByType;

	private Collection<Object> newRelatedObjects;

	RelationshipHandler(Neo4jPersistentProperty property, @Nullable Object rawValue, Cardinality cardinality,
			Collection<Object> newRelatedObjects, Map<Object, Object> newRelatedObjectsByType) {
		this.property = property;
		this.rawValue = rawValue;
		this.cardinality = cardinality;
		this.newRelatedObjects = newRelatedObjects;
		this.newRelatedObjectsByType = newRelatedObjectsByType;
	}

	static RelationshipHandler forProperty(Neo4jPersistentProperty property, @Nullable Object rawValue) {

		Cardinality cardinality;
		Collection<Object> newRelationshipObjectCollection = Collections.emptyList();
		Map<Object, Object> newRelationshipObjectCollectionMap = Collections.emptyMap();

		// Order is important here, all map based associations are dynamic, but not all
		// dynamic associations are one to many
		if (property.isCollectionLike()) {
			cardinality = Cardinality.ONE_TO_MANY;
			var size = (rawValue != null) ? ((Collection<?>) rawValue).size() : DEFAULT_SIZE;
			newRelationshipObjectCollection = CollectionFactory.createCollection(property.getType(), size);
		}
		else if (property.isDynamicOneToManyAssociation()) {
			cardinality = Cardinality.DYNAMIC_ONE_TO_MANY;
			var size = (rawValue != null) ? ((Map<?, ?>) rawValue).size() : DEFAULT_SIZE;
			newRelationshipObjectCollectionMap = CollectionFactory.createMap(property.getType(), size);
		}
		else if (property.isDynamicAssociation()) {
			cardinality = Cardinality.DYNAMIC_ONE_TO_ONE;
			var size = (rawValue != null) ? ((Map<?, ?>) rawValue).size() : DEFAULT_SIZE;
			newRelationshipObjectCollectionMap = CollectionFactory.createMap(property.getType(), size);
		}
		else {
			cardinality = Cardinality.ONE_TO_ONE;
		}

		return new RelationshipHandler(property, rawValue, cardinality, newRelationshipObjectCollection,
				newRelationshipObjectCollectionMap);
	}

	void handle(Object relatedValueToStore, Object newRelatedObject, Object potentiallyRecreatedRelatedObject) {

		if (potentiallyRecreatedRelatedObject != newRelatedObject) {
			if (this.cardinality == Cardinality.ONE_TO_ONE) {
				this.newRelatedObjects = Collections.singletonList(potentiallyRecreatedRelatedObject);
			}
			else if (this.cardinality == Cardinality.ONE_TO_MANY) {
				this.newRelatedObjects.add(potentiallyRecreatedRelatedObject);
			}
			else {
				Object key = ((Map.Entry<?, ?>) relatedValueToStore).getKey();
				if (this.cardinality == Cardinality.DYNAMIC_ONE_TO_ONE) {
					this.newRelatedObjectsByType.put(key, potentiallyRecreatedRelatedObject);
				}
				else {
					@SuppressWarnings("unchecked")
					Collection<Object> newCollection = (Collection<Object>) this.newRelatedObjectsByType
						.computeIfAbsent(key, k -> {
							Collection<?> objects = (this.rawValue != null)
									? (Collection<?>) ((Map<?, ?>) this.rawValue).get(key) : null;
							return CollectionFactory.createCollection(
									this.property.getTypeInformation().getRequiredActualType().getType(),
									(objects != null) ? objects.size() : DEFAULT_SIZE);
						});
					newCollection.add(potentiallyRecreatedRelatedObject);
				}
			}
		}
	}

	void applyFinalResultToOwner(PersistentPropertyAccessor<?> parentPropertyAccessor) {

		Object finalRelation = null;
		switch (this.cardinality) {
			case ONE_TO_ONE:
				finalRelation = Optional.ofNullable(this.newRelatedObjects)
					.flatMap(v -> v.stream().findFirst())
					.orElse(null);
				break;
			case ONE_TO_MANY:
				if (!this.newRelatedObjects.isEmpty()) {
					finalRelation = this.newRelatedObjects;
				}
				break;
			case DYNAMIC_ONE_TO_ONE:
			case DYNAMIC_ONE_TO_MANY:
				if (!this.newRelatedObjectsByType.isEmpty()) {
					finalRelation = this.newRelatedObjectsByType;
				}
				break;
		}

		if (finalRelation != null) {
			parentPropertyAccessor.setProperty(this.property, finalRelation);
		}
	}

	enum Cardinality {

		ONE_TO_ONE, ONE_TO_MANY, DYNAMIC_ONE_TO_ONE, DYNAMIC_ONE_TO_MANY

	}

}
