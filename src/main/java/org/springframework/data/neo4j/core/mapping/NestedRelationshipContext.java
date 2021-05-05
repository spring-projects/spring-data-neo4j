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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apiguardian.api.API;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.lang.Nullable;

/**
 * Working on nested relationships happens in a certain algorithmic context. This context enables a tight cohesion
 * between the algorithmic steps and the data, these steps are performed on. In our the interaction happens between the
 * data that describes the relationship and the specific steps of the algorithm.
 *
 * @author Philipp Tölle
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class NestedRelationshipContext {
	private final Neo4jPersistentProperty inverse;
	private final Object value;
	private final RelationshipDescription relationship;

	private final boolean inverseValueIsEmpty;

	private NestedRelationshipContext(Neo4jPersistentProperty inverse, @Nullable Object value,
			RelationshipDescription relationship, boolean inverseValueIsEmpty) {
		this.inverse = inverse;
		this.value = value;
		this.relationship = relationship;
		this.inverseValueIsEmpty = inverseValueIsEmpty;
	}

	public boolean isReadOnly() {
		return inverse.isAnnotationPresent(ReadOnlyProperty.class);
	}

	public Neo4jPersistentProperty getInverse() {
		return inverse;
	}

	@Nullable
	public Object getValue() {
		return value;
	}

	public RelationshipDescription getRelationship() {
		return relationship;
	}

	public boolean inverseValueIsEmpty() {
		return inverseValueIsEmpty;
	}

	boolean hasRelationshipWithProperties() {
		return this.relationship.hasRelationshipProperties();
	}

	public Object identifyAndExtractRelationshipTargetNode(Object relatedValue) {
		Object valueToBeSaved = relatedValue;
		if (relatedValue instanceof Map.Entry) {
			Map.Entry<?, ?> relatedValueMapEntry = (Map.Entry<?, ?>) relatedValue;
			if (this.hasRelationshipWithProperties()) {
				Object mapValue = ((Map.Entry<?, ?>) relatedValue).getValue();
				// it can be either a scalar entity holder or a list of it
				mapValue = mapValue instanceof List ? ((List<?>) mapValue).get(0) : mapValue;
				valueToBeSaved = ((MappingSupport.RelationshipPropertiesWithEntityHolder) mapValue).getRelatedEntity();
			} else if (this.getInverse().isDynamicAssociation()) {
				valueToBeSaved = relatedValueMapEntry.getValue();
			}
		} else if (this.hasRelationshipWithProperties()) {
			// here comes the entity
			valueToBeSaved = ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValue).getRelatedEntity();
		}

		return valueToBeSaved;
	}

	public @Nullable PersistentPropertyAccessor<?> getRelationshipPropertiesPropertyAccessor(Object relatedValue) {

		if (!this.hasRelationshipWithProperties() || relatedValue == null) {
			return null;
		}

		if (relatedValue instanceof Map.Entry) {
			Object mapValue = ((Map.Entry<?, ?>) relatedValue).getValue();
			mapValue = mapValue instanceof List ? ((List<?>) mapValue).get(0) : mapValue;
			return ((MappingSupport.RelationshipPropertiesWithEntityHolder) mapValue).getRelationshipPropertiesPropertyAccessor();
		} else {
			return ((MappingSupport.RelationshipPropertiesWithEntityHolder) relatedValue).getRelationshipPropertiesPropertyAccessor();
		}
	}

	public static NestedRelationshipContext of(Association<Neo4jPersistentProperty> handler,
			PersistentPropertyAccessor<?> propertyAccessor, Neo4jPersistentEntity<?> neo4jPersistentEntity) {

		Neo4jPersistentProperty inverse = handler.getInverse();

		// value can be a collection or scalar of related notes, point to a relationship property (scalar or collection)
		// or is a dynamic relationship (map)
		Object value = propertyAccessor.getProperty(inverse);
		boolean inverseValueIsEmpty = value == null;

		RelationshipDescription relationship = neo4jPersistentEntity.getRelationshipsInHierarchy((pp -> true)).stream()
				.filter(r -> r.getFieldName().equals(inverse.getName())).findFirst().get();

		if (relationship.hasRelationshipProperties() && value != null) {
			Neo4jPersistentEntity<?> relationshipPropertiesEntity = (Neo4jPersistentEntity<?>) relationship.getRelationshipPropertiesEntity();

			// If this is dynamic relationship (Map<Object, Object>), extract the keys as relationship names
			// and the map values as values.
			// The values themself can be either a scalar or a List.
			if (relationship.isDynamic()) {
				Map<Object, Object> relationshipProperties = new HashMap<>();
				for (Map.Entry<Object, Object> mapEntry : ((Map<Object, Object>) value).entrySet()) {
					List<MappingSupport.RelationshipPropertiesWithEntityHolder> relationshipValues = new ArrayList<>();
					// register the relationship type as key
					relationshipProperties.put(mapEntry.getKey(), relationshipValues);
					Object mapEntryValue = mapEntry.getValue();

					if (mapEntryValue instanceof List) {
						for (Object relationshipProperty : ((List<Object>) mapEntryValue)) {
							MappingSupport.RelationshipPropertiesWithEntityHolder oneOfThem =
									new MappingSupport.RelationshipPropertiesWithEntityHolder(
											relationshipPropertiesEntity, relationshipProperty,
											getTargetNode(relationshipPropertiesEntity, relationshipProperty));
							relationshipValues.add(oneOfThem);
						}
					} else { // scalar
						MappingSupport.RelationshipPropertiesWithEntityHolder oneOfThem =
								new MappingSupport.RelationshipPropertiesWithEntityHolder(
										relationshipPropertiesEntity, mapEntryValue,
										getTargetNode(relationshipPropertiesEntity, mapEntryValue));
						relationshipProperties.put(mapEntry.getKey(), oneOfThem);
					}

				}
				value = relationshipProperties;
			} else {
				if (inverse.isCollectionLike()) {
					List<MappingSupport.RelationshipPropertiesWithEntityHolder> relationshipProperties = new ArrayList<>();
					for (Object relationshipProperty : ((Collection<Object>) value)) {

						MappingSupport.RelationshipPropertiesWithEntityHolder oneOfThem =
								new MappingSupport.RelationshipPropertiesWithEntityHolder(
										relationshipPropertiesEntity, relationshipProperty,
										getTargetNode(relationshipPropertiesEntity, relationshipProperty));
						relationshipProperties.add(oneOfThem);
					}
					value = relationshipProperties;
				} else {
					value = new MappingSupport.RelationshipPropertiesWithEntityHolder(relationshipPropertiesEntity,
							value,
							getTargetNode(relationshipPropertiesEntity, value));
				}
			}
		}

		return new NestedRelationshipContext(inverse, value, relationship, inverseValueIsEmpty);
	}

	private static Object getTargetNode(Neo4jPersistentEntity<?> relationshipPropertiesEntity, Object object) {

		PersistentPropertyAccessor<Object> propertyAccessor = relationshipPropertiesEntity.getPropertyAccessor(object);
		return propertyAccessor.getProperty(relationshipPropertiesEntity.getPersistentProperty(TargetNode.class));

	}
}
