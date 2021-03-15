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
package org.springframework.data.neo4j.core.mapping;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Type;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.lang.Nullable;
import org.springframework.data.mapping.PersistentPropertyAccessor;

/**
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class MappingSupport {

	/**
	 * The value for a relationship can be a scalar object (1:1), a collection (1:n), a map (1:n, but with dynamic
	 * relationship types) or a map (1:n) with properties for each relationship. This method unifies the type into
	 * something iterable, depending on the given inverse type.
	 *
	 * @param rawValue The raw value to unify
	 * @return A unified collection (Either a collection of Map.Entry for dynamic and relationships with properties or a
	 *         list of related values)
	 */
	public static Collection<?> unifyRelationshipValue(Neo4jPersistentProperty property, @Nullable Object rawValue) {

		if (rawValue == null) {
			return Collections.emptyList();
		}

		Collection<?> unifiedValue;
		if (property.isDynamicAssociation()) {
			if (property.isDynamicOneToManyAssociation()) {
				unifiedValue = ((Map<?, Collection<?>>) rawValue)
						.entrySet().stream()
						.flatMap(e -> e.getValue().stream().map(v -> new SimpleEntry(e.getKey(), v)))
						.collect(Collectors.toList());
			} else {
				unifiedValue = ((Map<?, Object>) rawValue).entrySet();
			}
		} else if (property.isCollectionLike()) {
			unifiedValue = (Collection<Object>) rawValue;
		} else {
			unifiedValue = Collections.singleton(rawValue);
		}
		return unifiedValue;
	}

	/**
	 * A helper that produces a predicate to check whether a {@link Value} is a list value and contains only other
	 * values with a given type.
	 *
	 * @param collectionType The required collection type system
	 * @param requiredType   The required type
	 * @return A predicate
	 */
	public static Predicate<Value> isListContainingOnly(Type collectionType, Type requiredType) {

		Predicate<Value> containsOnlyRequiredType = entry -> {
			for (Value listEntry : entry.values()) {
				if (!listEntry.hasType(requiredType)) {
					return false;
				}
			}
			return true;
		};

		Predicate<Value> isList = entry -> entry.hasType(collectionType);
		return isList.and(containsOnlyRequiredType);
	}

	/**
	 * Extract the relationship properties or just the related object if there are no relationship properties
	 * attached.
	 *
	 * @param neo4jMappingContext - current mapping context
	 * @param hasRelationshipProperties - does this relationship has properties
	 * @param isDynamicAssociation - is the defined relationship a dynamic association
	 * @param valueToStore - either a plain object or {@link RelationshipPropertiesWithEntityHolder}
	 * @param propertyAccessor - PropertyAccessor for the value
	 *
	 * @return extracted related object or relationship properties
	 */
	public static Object getRelationshipOrRelationshipPropertiesObject(Neo4jMappingContext neo4jMappingContext,
																	   boolean hasRelationshipProperties,
																	   boolean isDynamicAssociation,
																	   Object valueToStore,
																	   PersistentPropertyAccessor<?> propertyAccessor) {

		Object newRelationshipObject = propertyAccessor.getBean();
		if (hasRelationshipProperties) {
			MappingSupport.RelationshipPropertiesWithEntityHolder entityHolder =
					(RelationshipPropertiesWithEntityHolder)
							(isDynamicAssociation
								? ((Map.Entry<Object, Object>) valueToStore).getValue()
								: valueToStore);

			Object relationshipPropertiesValue = entityHolder.getRelationshipProperties();

			Neo4jPersistentEntity<?> persistentEntity =
					neo4jMappingContext.getPersistentEntity(relationshipPropertiesValue.getClass());

			PersistentPropertyAccessor<Object> relationshipPropertiesAccessor = persistentEntity.getPropertyAccessor(relationshipPropertiesValue);
			relationshipPropertiesAccessor.setProperty(persistentEntity.getPersistentProperty(TargetNode.class), newRelationshipObject);
			newRelationshipObject = relationshipPropertiesAccessor.getBean();
		}
		return newRelationshipObject;
	}

	/**
	 * Adds previously created objects of related entities to a map.
	 * The main purpose of this method is to provide a merge function that can collect multiple value for the same
	 * key but in different calls under the same key.
	 */
	public static void addToDynamicAssociationCollection(Neo4jPersistentProperty relationshipProperty,
														 Map.Entry<Object, Object> relatedValueToStore,
														 Object newRelationshipObject,
														 Collection<Object> newRelationshipObjectCollection,
												  		 Map<Object, Object> newRelationshipObjectCollectionMap) {

		Object key = relatedValueToStore.getKey();
		Object value;
		if (relationshipProperty.isDynamicOneToManyAssociation()) {
			value = newRelationshipObjectCollection;
		} else {
			value = newRelationshipObject;
		}

		newRelationshipObjectCollectionMap.merge(key, value, (existingElement, additionalElement) -> {

			if (existingElement instanceof Collection) {
				((Collection<Object>) existingElement).addAll((Collection<Object>) additionalElement);
				return existingElement;
			}

			ArrayList<Object> objects = new ArrayList<>();
			objects.add(existingElement);
			objects.add(additionalElement);
			return objects;
		});
	}

	private MappingSupport() {}

	/**
	 * Class that defines a tuple of relationship with properties and the connected target entity.
	 */
	@API(status = API.Status.INTERNAL)
	public final static class RelationshipPropertiesWithEntityHolder {
		private final PersistentPropertyAccessor<?> relationshipPropertiesPropertyAccessor;
		private final Object relationshipProperties;
		private final Object relatedEntity;

		RelationshipPropertiesWithEntityHolder(
				Neo4jPersistentEntity<?> relationshipPropertiesEntity,
				Object relationshipProperties, Object relatedEntity
		) {
			this.relationshipPropertiesPropertyAccessor = relationshipPropertiesEntity.getPropertyAccessor(relationshipProperties);
			this.relationshipProperties = relationshipProperties;
			this.relatedEntity = relatedEntity;
		}

		public PersistentPropertyAccessor<?> getRelationshipPropertiesPropertyAccessor() {
			return relationshipPropertiesPropertyAccessor;
		}

		public Object getRelationshipProperties() {
			return relationshipProperties;
		}

		public Object getRelatedEntity() {
			return relatedEntity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			RelationshipPropertiesWithEntityHolder that = (RelationshipPropertiesWithEntityHolder) o;
			return relationshipProperties.equals(that.relationshipProperties) && relatedEntity.equals(that.relatedEntity);
		}

		@Override
		public int hashCode() {
			return Objects.hash(relationshipProperties, relatedEntity);
		}
	}
}
