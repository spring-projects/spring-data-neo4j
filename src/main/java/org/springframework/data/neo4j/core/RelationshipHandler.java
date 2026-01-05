/*
 * Copyright 2011-present the original author or authors.
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

import org.apiguardian.api.API;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Internal helper class that takes care of tracking whether a related object or a collection of related objects was recreated
 * due to changing immutable properties
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "6.1")
final class RelationshipHandler {

	enum Cardinality {

		ONE_TO_ONE,
		ONE_TO_MANY,
		DYNAMIC_ONE_TO_ONE,
		DYNAMIC_ONE_TO_MANY
	}

	static RelationshipHandler forProperty(Neo4jPersistentProperty property, Object rawValue) {

		Cardinality cardinality;
		Collection<Object> newRelationshipObjectCollection = Collections.emptyList();
		Map<Object, Object> newRelationshipObjectCollectionMap = Collections.emptyMap();

		// Order is important here, all map based associations are dynamic, but not all dynamic associations are one to many
		if (property.isCollectionLike()) {
			cardinality = Cardinality.ONE_TO_MANY;
			newRelationshipObjectCollection = CollectionFactory.createCollection(property.getType(), ((Collection<?>) rawValue).size());
		} else if (property.isDynamicOneToManyAssociation()) {
			cardinality = Cardinality.DYNAMIC_ONE_TO_MANY;
			newRelationshipObjectCollectionMap = CollectionFactory.createMap(property.getType(), ((Map<?, ?>) rawValue).size());
		} else if (property.isDynamicAssociation()) {
			cardinality = Cardinality.DYNAMIC_ONE_TO_ONE;
			newRelationshipObjectCollectionMap = CollectionFactory.createMap(property.getType(), ((Map<?, ?>) rawValue).size());
		} else {
			cardinality = Cardinality.ONE_TO_ONE;
		}

		return new RelationshipHandler(property, rawValue, cardinality, newRelationshipObjectCollection, newRelationshipObjectCollectionMap);
	}

	private final Neo4jPersistentProperty property;
	/**
	 * The raw value as passed to the template.
	 */
	private final Object rawValue;
	private final Cardinality cardinality;

	private Collection<Object> newRelatedObjects;
	private final Map<Object, Object> newRelatedObjectsByType;

	RelationshipHandler(Neo4jPersistentProperty property,
						Object rawValue, Cardinality cardinality,
						Collection<Object> newRelatedObjects,
						Map<Object, Object> newRelatedObjectsByType) {
		this.property = property;
		this.rawValue = rawValue;
		this.cardinality = cardinality;
		this.newRelatedObjects = newRelatedObjects;
		this.newRelatedObjectsByType = newRelatedObjectsByType;
	}

	void handle(Object relatedValueToStore, Object newRelatedObject, Object potentiallyRecreatedRelatedObject) {

		if (potentiallyRecreatedRelatedObject != newRelatedObject) {
			if (cardinality == Cardinality.ONE_TO_ONE) {
				this.newRelatedObjects = Collections.singletonList(potentiallyRecreatedRelatedObject);
			} else if (cardinality == Cardinality.ONE_TO_MANY) {
				newRelatedObjects.add(potentiallyRecreatedRelatedObject);
			} else {
				Object key = ((Map.Entry<?, ?>) relatedValueToStore).getKey();
				if (cardinality == Cardinality.DYNAMIC_ONE_TO_ONE) {
					newRelatedObjectsByType.put(key, potentiallyRecreatedRelatedObject);
				} else {
					@SuppressWarnings("unchecked")
					Collection<Object> newCollection = (Collection<Object>) newRelatedObjectsByType
							.computeIfAbsent(key, k -> CollectionFactory.createCollection(
									property.getTypeInformation().getRequiredActualType().getType(),
									((Collection<?>) ((Map<?, ?>) rawValue).get(key)).size()));
					newCollection.add(potentiallyRecreatedRelatedObject);
				}
			}
		}
	}

	void applyFinalResultToOwner(PersistentPropertyAccessor<?> parentPropertyAccessor) {

		Object finalRelation = null;
		switch (cardinality) {
			case ONE_TO_ONE:
				finalRelation = Optional.ofNullable(newRelatedObjects).flatMap(v -> v.stream().findFirst()).orElse(null);
				break;
			case ONE_TO_MANY:
				if (!newRelatedObjects.isEmpty()) {
					finalRelation = newRelatedObjects;
				}
				break;
			case DYNAMIC_ONE_TO_ONE:
			case DYNAMIC_ONE_TO_MANY:
				if (!newRelatedObjectsByType.isEmpty()) {
					finalRelation = newRelatedObjectsByType;
				}
				break;
		}

		if (finalRelation != null) {
			parentPropertyAccessor.setProperty(property, finalRelation);
		}
	}
}
