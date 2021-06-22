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

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.convert.Neo4jSimpleTypes;
import org.springframework.data.neo4j.core.mapping.GraphPropertyDescription;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible for creating a List of {@link PropertyPath} entries that contains all reachable
 * properties (w/o circles).
 */
public class PropertyFilterSupport {

	public static List<PropertyPath> getInputProperties(ResultProcessor resultProcessor, ProjectionFactory factory, Neo4jMappingContext mappingContext) {

		ReturnedType returnedType = resultProcessor.getReturnedType();
		List<PropertyPath> filteredProperties = new ArrayList<>();

		for (String inputProperty : returnedType.getInputProperties()) {
			if (returnedType.isProjecting()) {
				addPropertiesFrom(returnedType.getDomainType(), returnedType.getReturnedType(), factory, filteredProperties, inputProperty, mappingContext);
			} else {
				addPropertiesFromEntity(filteredProperties, PropertyPath.from(inputProperty, returnedType.getDomainType()), returnedType.getReturnedType(), mappingContext, new HashSet<>());
			}
		}
		return returnedType.isProjecting() ? filteredProperties : Collections.emptyList();
	}

	public static void addPropertiesFrom(Class<?> domainType, Class<?> returnedType, ProjectionFactory factory, Collection<PropertyPath> filteredProperties, String inputProperty, Neo4jMappingContext mappingContext) {
		ProjectionInformation projectionInformation1 = factory.getProjectionInformation(returnedType);
		PropertyPath propertyPath;
		if (projectionInformation1.isClosed()) {
			propertyPath = PropertyPath.from(inputProperty, returnedType);
		} else {
			propertyPath = PropertyPath.from(inputProperty, domainType);
		}

		Class<?> propertyType = propertyPath.getLeafType();
		if (Neo4jSimpleTypes.HOLDER.isSimpleType(propertyType) || mappingContext.hasCustomWriteTarget(propertyType)) {
			filteredProperties.add(propertyPath);
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			if (propertyType.equals(domainType)) {
				return;
			}
			processEntity(domainType, filteredProperties, inputProperty, mappingContext);

		} else {
			ProjectionInformation nestedProjectionInformation = factory.getProjectionInformation(propertyType);
			filteredProperties.add(propertyPath);
			if (nestedProjectionInformation.isClosed()) {
				for (PropertyDescriptor secondInputProperty : nestedProjectionInformation.getInputProperties()) {
					PropertyPath nestedPropertyPath = propertyPath.nested(secondInputProperty.getName());
					filteredProperties.add(nestedPropertyPath);
					addPropertiesFrom(domainType, returnedType, factory, filteredProperties, nestedPropertyPath.toDotPath(), mappingContext);
				}
			} else {
				// an open projection at this place needs to get replaced with the matching (real) entity
				processEntity(domainType, filteredProperties, inputProperty, mappingContext);
			}
		}
	}

	private static void processEntity(Class<?> domainType, Collection<PropertyPath> filteredProperties, String inputProperty, Neo4jMappingContext mappingContext) {
		PropertyPath propertyPath;
		Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainType);
		Neo4jPersistentProperty persistentProperty = persistentEntity.getPersistentProperty(inputProperty);
		Class<?> propertyEntityType = persistentProperty.getActualType();
		propertyPath = PropertyPath.from(inputProperty, domainType);
		addPropertiesFromEntity(filteredProperties, propertyPath, propertyEntityType, mappingContext, new HashSet<>());
	}

	private static void addPropertiesFromEntity(Collection<PropertyPath> filteredProperties, PropertyPath propertyPath, Class<?> propertyType, Neo4jMappingContext mappingContext, Collection<Neo4jPersistentEntity<?>> irgendeineListe) {
		Neo4jPersistentEntity<?> persistentEntityFromProperty = mappingContext.getPersistentEntity(propertyType);
		if (hasProcessedEntity(persistentEntityFromProperty, irgendeineListe)) {
			return;
		}
		irgendeineListe.add(persistentEntityFromProperty);
		if (mappingContext.hasPersistentEntityFor(propertyPath.getOwningType().getType())) {
			irgendeineListe.add(mappingContext.getPersistentEntity(propertyPath.getOwningType().getType()));
		}

		takeAllPropertiesFromEntity(filteredProperties, propertyPath, mappingContext, persistentEntityFromProperty, irgendeineListe);
	}

	private static boolean hasProcessedEntity(Neo4jPersistentEntity<?> persistentEntityFromProperty, Collection<Neo4jPersistentEntity<?>> irgendeineListe) {
		return irgendeineListe.contains(persistentEntityFromProperty);
	}

	private static void takeAllPropertiesFromEntity(Collection<PropertyPath> filteredProperties, PropertyPath propertyPath, Neo4jMappingContext mappingContext, Neo4jPersistentEntity<?> persistentEntityFromProperty, Collection<Neo4jPersistentEntity<?>> irgendeineListe) {
		List<String> propertyNames = persistentEntityFromProperty.getGraphProperties().stream().map(GraphPropertyDescription::getFieldName).collect(Collectors.toList());
		filteredProperties.add(propertyPath);

		for (String propertyName : propertyNames) {
			addPropertiesFrom(filteredProperties, propertyPath.nested(propertyName), mappingContext, irgendeineListe);
		}
		propertyNames = persistentEntityFromProperty.getRelationships().stream().map(RelationshipDescription::getFieldName).collect(Collectors.toList());
		for (String propertyName : propertyNames) {
			addPropertiesFrom(filteredProperties, propertyPath.nested(propertyName), mappingContext, irgendeineListe);
		}
	}

	private static void addPropertiesFrom(Collection<PropertyPath> filteredProperties, PropertyPath propertyPath, Neo4jMappingContext mappingContext, Collection<Neo4jPersistentEntity<?>> irgendeineListe) {
		if (filteredProperties.contains(propertyPath)) {
			return;
		}
		Class<?> propertyType = propertyPath.getLeafType();
		if (Neo4jSimpleTypes.HOLDER.isSimpleType(propertyType) || mappingContext.hasCustomWriteTarget(propertyType)) {
			filteredProperties.add(propertyPath);
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			addPropertiesFromEntity(filteredProperties, propertyPath, propertyType, mappingContext, irgendeineListe);
		}
	}
}
