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

import java.util.Collection;
import java.util.Map;

import org.apiguardian.api.API;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;

/**
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "6.1")
enum RelationshipCardinality {

	ONE_TO_ONE,
	ONE_TO_MANY,
	DYNAMIC_ONE_TO_ONE,
	DYNAMIC_ONE_TO_MANY;

	static class Dingens {

		static Dingens forProperty(Neo4jPersistentProperty property, Object rawValue) {

			RelationshipCardinality cardinality;
			Object newRelationshipObject = null;
			Collection<Object> newRelationshipObjectCollection = null;
			Map<Object, Object> newRelationshipObjectCollectionMap = null;

			// Order is important here, all map based associations are dynamic, but not all dynamic associations are one to many
			if (property.isCollectionLike()) {
				cardinality = RelationshipCardinality.ONE_TO_MANY;
				newRelationshipObjectCollection = CollectionFactory
						.createApproximateCollection(rawValue, ((Collection<?>) rawValue).size());
			} else if (property.isDynamicOneToManyAssociation()) {
				cardinality = RelationshipCardinality.DYNAMIC_ONE_TO_MANY;
				newRelationshipObjectCollectionMap = CollectionFactory
						.createApproximateMap(rawValue, ((Map<?, ?>) rawValue).size());
			} else if (property.isDynamicAssociation()) {
				cardinality = RelationshipCardinality.DYNAMIC_ONE_TO_ONE;
				newRelationshipObjectCollectionMap = CollectionFactory
						.createApproximateMap(rawValue, ((Map<?, ?>) rawValue).size());
			} else {
				cardinality = RelationshipCardinality.ONE_TO_ONE;
			}

			return new Dingens(property, rawValue, cardinality, newRelationshipObjectCollection,
					newRelationshipObjectCollectionMap);
		}

		private final Neo4jPersistentProperty property;
		/**
		 * The raw value as passed to the template.
		 */
		private final Object rawValue;
		private final RelationshipCardinality cardinality;
		private Object newRelatedObject = null;
		private final Collection<Object> newRelatedObjects;
		private final Map<Object, Object> newRelatedObjectsByType;

		Dingens(Neo4jPersistentProperty property,
				Object rawValue, RelationshipCardinality cardinality,
				Collection<Object> newRelatedObjects,
				Map<Object, Object> newRelatedObjectsByType) {
			this.property = property;
			this.rawValue = rawValue;
			this.cardinality = cardinality;
			this.newRelatedObjects = newRelatedObjects;
			this.newRelatedObjectsByType = newRelatedObjectsByType;
		}

		void handle(Object relatedValueToStore, Object newRelatedObject, Object potentiallyModifiedNewRelatedObject) {
			if (potentiallyModifiedNewRelatedObject == newRelatedObject) {
				return;
			} else if (cardinality == RelationshipCardinality.ONE_TO_ONE) {
				this.newRelatedObject = potentiallyModifiedNewRelatedObject;
			} else if (cardinality == RelationshipCardinality.ONE_TO_MANY) {
				newRelatedObjects.add(potentiallyModifiedNewRelatedObject);
			} else {
				Object key = ((Map.Entry<Object, Object>) relatedValueToStore).getKey();
				if (cardinality == RelationshipCardinality.DYNAMIC_ONE_TO_ONE) {
					newRelatedObjectsByType.put(key, potentiallyModifiedNewRelatedObject);
				} else {
					Collection<Object> newCollection = (Collection<Object>) newRelatedObjectsByType
							.computeIfAbsent(key, k -> CollectionFactory.createCollection(
									property.getTypeInformation().getRequiredActualType().getType(),
									((Collection) ((Map) rawValue).get(key)).size()));
					newCollection.add(potentiallyModifiedNewRelatedObject);
				}
			}
		}

		void applyFinalResultToOwner(PersistentPropertyAccessor<?> parentPropertyAccessor) {
			Object finalRelation = null;
			switch (cardinality) {
				case ONE_TO_ONE:
					finalRelation = newRelatedObject;
					break;
				case ONE_TO_MANY:
					if (!newRelatedObjects.isEmpty()) {
						finalRelation = newRelatedObjects;
					}
					break;
				case DYNAMIC_ONE_TO_ONE:
				case DYNAMIC_ONE_TO_MANY:
					if(!newRelatedObjectsByType.isEmpty()) {
						finalRelation = newRelatedObjectsByType;
					}
					break;
			}

			if (finalRelation != null) {
				parentPropertyAccessor.setProperty(property, finalRelation);
			}
		}
	}
}
