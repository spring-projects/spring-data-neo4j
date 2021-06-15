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
 * Tell me more...
 */
public class PropertyFilterSupport {

	public static List<PropertyPath> getInputProperties(ResultProcessor resultProcessor, ProjectionFactory factory, Neo4jMappingContext mappingContext) {

		ReturnedType returnedType = resultProcessor.getReturnedType();
		List<PropertyPath> filteredProperties = new ArrayList<>();

		for (String inputProperty : returnedType.getInputProperties()) {
			if (returnedType.isProjecting()) {
				if (returnedType.getReturnedType().isInterface() && factory.getProjectionInformation(returnedType.getReturnedType()).isClosed()) {
					addPropertiesFrom(returnedType.getDomainType(), factory, returnedType.getDomainType(), filteredProperties, inputProperty, mappingContext);
				} else {
					addPropertiesFrom(returnedType.getReturnedType(), factory, returnedType.getDomainType(), filteredProperties, inputProperty, mappingContext);
				}
			} else {
				addPropertiesFromEntity(returnedType.getReturnedType(), filteredProperties, PropertyPath.from(inputProperty, returnedType.getDomainType()), returnedType.getReturnedType(), mappingContext, new HashSet<>());
			}
		}
		return returnedType.isProjecting() ? filteredProperties : Collections.emptyList();
	}

	public static void addPropertiesFrom(Class<?> chef, ProjectionFactory factory, Class<?> type, Collection<PropertyPath> filteredProperties, String inputProperty, Neo4jMappingContext mappingContext) {
		ProjectionInformation projectionInformation1 = factory.getProjectionInformation(type);
		PropertyPath propertyPath;
		if (projectionInformation1.isClosed() && type.isInterface()) {
			propertyPath = PropertyPath.from(inputProperty, type);
		} else {
			propertyPath = PropertyPath.from(inputProperty, chef);
		}

		Class<?> leafType = propertyPath.getLeafType();
		if (Neo4jSimpleTypes.HOLDER.isSimpleType(leafType) || mappingContext.hasCustomWriteTarget(leafType)) {
			filteredProperties.add(propertyPath);
		} else if (mappingContext.hasPersistentEntityFor(leafType)) {
			addPropertiesFromEntity(chef, filteredProperties, propertyPath, leafType, mappingContext, new HashSet<>());
		} else {
			ProjectionInformation projectionInformation = factory.getProjectionInformation(leafType);
			filteredProperties.add(propertyPath);
			if (projectionInformation.isClosed()) {
				for (PropertyDescriptor secondInputProperty : projectionInformation.getInputProperties()) {
					PropertyPath nestedPropertyPath = propertyPath.nested(secondInputProperty.getName());
					filteredProperties.add(nestedPropertyPath);
					addPropertiesFrom(chef, factory, type, filteredProperties, nestedPropertyPath.toDotPath(), mappingContext);
				}
			} else {
				// an open projection needs to get replaced with the matching (real) entity
				Class<?> propertyEntityType =  mappingContext.getPersistentEntity(type).getPersistentProperty(inputProperty).getActualType();
				addPropertiesFromEntity(chef, filteredProperties, propertyPath, propertyEntityType, mappingContext, new HashSet<>());
			}
		}
	}

	private static void addPropertiesFromEntity(Class<?> chef, Collection<PropertyPath> filteredProperties, PropertyPath propertyPath, Class<?> leafType, Neo4jMappingContext mappingContext, Collection<Neo4jPersistentEntity<?>> irgendeineListe) {
		Neo4jPersistentEntity<?> persistentEntityFromProperty = mappingContext.getPersistentEntity(leafType);
		if (hasProcessedEntity(persistentEntityFromProperty, irgendeineListe)) {
			return;
		}
		irgendeineListe.add(persistentEntityFromProperty);
		takeAllPropertiesFromEntity(chef, filteredProperties, propertyPath, mappingContext, persistentEntityFromProperty, irgendeineListe);
	}

	private static boolean hasProcessedEntity(Neo4jPersistentEntity<?> persistentEntityFromProperty, Collection<Neo4jPersistentEntity<?>> irgendeineListe) {
		return irgendeineListe.contains(persistentEntityFromProperty);
	}

	private static void takeAllPropertiesFromEntity(Class<?> chef, Collection<PropertyPath> filteredProperties, PropertyPath propertyPath, Neo4jMappingContext mappingContext, Neo4jPersistentEntity<?> persistentEntityFromProperty, Collection<Neo4jPersistentEntity<?>> irgendeineListe) {
		List<String> propertyNames = persistentEntityFromProperty.getGraphProperties().stream().map(GraphPropertyDescription::getFieldName).collect(Collectors.toList());
		filteredProperties.add(propertyPath);

		for (String propertyName : propertyNames) {
			addPropertiesFrom(chef, filteredProperties, propertyPath.nested(propertyName).toDotPath(), mappingContext, irgendeineListe);
		}
		propertyNames = persistentEntityFromProperty.getRelationships().stream().map(RelationshipDescription::getFieldName).collect(Collectors.toList());
		for (String propertyName : propertyNames) {
			addPropertiesFrom(chef, filteredProperties, propertyPath.nested(propertyName).toDotPath(), mappingContext, irgendeineListe);
		}
	}

	private static void addPropertiesFrom(Class<?> chef, Collection<PropertyPath> filteredProperties, String inputProperty, Neo4jMappingContext mappingContext, Collection<Neo4jPersistentEntity<?>> irgendeineListe) {
		PropertyPath propertyPath = PropertyPath.from(inputProperty, chef);
		if (filteredProperties.contains(propertyPath)) {
			return;
		}
		Class<?> leafType = propertyPath.getLeafType();
		if (Neo4jSimpleTypes.HOLDER.isSimpleType(leafType) || mappingContext.hasCustomWriteTarget(leafType)) {
			filteredProperties.add(propertyPath);
		} else if (mappingContext.hasPersistentEntityFor(leafType)) {
			addPropertiesFromEntity(chef, filteredProperties, propertyPath, leafType, mappingContext, irgendeineListe);
		}
	}
}
