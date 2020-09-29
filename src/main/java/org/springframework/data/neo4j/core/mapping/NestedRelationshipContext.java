/*
 * Copyright 2011-2020 the original author or authors.
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
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class NestedRelationshipContext {
	private final Neo4jPersistentProperty inverse;
	private final Object value;
	private final RelationshipDescription relationship;
	private final Class<?> associationTargetType;

	private final boolean inverseValueIsEmpty;

	private NestedRelationshipContext(Neo4jPersistentProperty inverse, @Nullable Object value,
			RelationshipDescription relationship, Class<?> associationTargetType, boolean inverseValueIsEmpty) {
		this.inverse = inverse;
		this.value = value;
		this.relationship = relationship;
		this.associationTargetType = associationTargetType;
		this.inverseValueIsEmpty = inverseValueIsEmpty;
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

	public Class<?> getAssociationTargetType() {
		return associationTargetType;
	}

	public boolean inverseValueIsEmpty() {
		return inverseValueIsEmpty;
	}

	boolean hasRelationshipWithProperties() {
		return this.relationship.hasRelationshipProperties();
	}

	public  Object identifyAndExtractRelationshipTargetNode(Object relatedValue) {
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

	public static NestedRelationshipContext of(Association<Neo4jPersistentProperty> handler,
			PersistentPropertyAccessor<?> propertyAccessor, Neo4jPersistentEntity<?> neo4jPersistentEntity) {

		Neo4jPersistentProperty inverse = handler.getInverse();

		boolean inverseValueIsEmpty = propertyAccessor.getProperty(inverse) == null;
		// value can be a collection or scalar of related notes, point to a relationship property (scalar or collection)
		// or is a dynamic relationship (map)
		Object value = propertyAccessor.getProperty(inverse);

		RelationshipDescription relationship = neo4jPersistentEntity.getRelationships().stream()
				.filter(r -> r.getFieldName().equals(inverse.getName())).findFirst().get();

		// if we have a relationship with properties, the targetNodeType is the map key
		Class<?> associationTargetType = inverse.getAssociationTargetType();

		if (relationship.hasRelationshipProperties() && value != null) {
			Neo4jPersistentEntity<?> relationshipPropertiesEntity = (Neo4jPersistentEntity<?>) relationship.getRelationshipPropertiesEntity();

			// If this is dynamic relationship (Map<Object, Object>), extract the keys as relationship names
			// and the map values as values.
			// The values themself can be either a scalar or a List.
			if (relationship.isDynamic()) {
				Map<Object, List<MappingSupport.RelationshipPropertiesWithEntityHolder>> relationshipProperties = new HashMap<>();
				for (Map.Entry<Object, Object> mapEntry : ((Map<Object, Object>) value).entrySet()) {
					List<MappingSupport.RelationshipPropertiesWithEntityHolder> relationshipValues = new ArrayList<>();
					// register the relationship type as key
					relationshipProperties.put(mapEntry.getKey(), relationshipValues);
					Object mapEntryValue = mapEntry.getValue();

					if (mapEntryValue instanceof List) {
						for (Object relationshipProperty : ((List<Object>) mapEntryValue)) {
							MappingSupport.RelationshipPropertiesWithEntityHolder oneOfThem =
									new MappingSupport.RelationshipPropertiesWithEntityHolder(relationshipProperty,
											getTargetNode(relationshipPropertiesEntity, relationshipProperty));
							relationshipValues.add(oneOfThem);
						}
					} else { // scalar
						MappingSupport.RelationshipPropertiesWithEntityHolder oneOfThem =
								new MappingSupport.RelationshipPropertiesWithEntityHolder(mapEntryValue,
										getTargetNode(relationshipPropertiesEntity, mapEntryValue));
						relationshipValues.add(oneOfThem);
					}

				}
				value = relationshipProperties;
			} else {
				List<MappingSupport.RelationshipPropertiesWithEntityHolder> relationshipProperties = new ArrayList<>();
				for (Object relationshipProperty : ((Collection<Object>) value)) {

					MappingSupport.RelationshipPropertiesWithEntityHolder oneOfThem =
							new MappingSupport.RelationshipPropertiesWithEntityHolder(relationshipProperty,
									getTargetNode(relationshipPropertiesEntity, relationshipProperty));
					relationshipProperties.add(oneOfThem);
				}
				value = relationshipProperties;
			}
		}

		return new NestedRelationshipContext(inverse, value, relationship, associationTargetType, inverseValueIsEmpty);
	}

	private static Object getTargetNode(Neo4jPersistentEntity<?> relationshipPropertiesEntity, Object object) {

		PersistentPropertyAccessor<Object> propertyAccessor = relationshipPropertiesEntity.getPropertyAccessor(object);
		return propertyAccessor.getProperty(relationshipPropertiesEntity.getPersistentProperty(TargetNode.class));

	}
}
