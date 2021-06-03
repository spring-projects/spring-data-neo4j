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
				addPropertiesFromProjection(factory, returnedType.getReturnedType(), filteredProperties, inputProperty, mappingContext);
			} else {
				filteredProperties.add(PropertyPath.from(inputProperty, returnedType.getDomainType()));
			}
		}
		return returnedType.isProjecting() ? filteredProperties : Collections.emptyList();
	}

	public static void addPropertiesFromProjection(ProjectionFactory factory, Class<?> type, Collection<PropertyPath> filteredProperties, String inputProperty, Neo4jMappingContext mappingContext) {
		PropertyPath propertyPath = PropertyPath.from(inputProperty, type);

		Class<?> leafType = propertyPath.getLeafType();
		if (Neo4jSimpleTypes.HOLDER.isSimpleType(leafType) || mappingContext.hasCustomWriteTarget(leafType)) {
			filteredProperties.add(propertyPath);
		} else if (mappingContext.hasPersistentEntityFor(leafType)) {
			addPropertiesFromEntity(filteredProperties, propertyPath, leafType, mappingContext);
		} else {
			ProjectionInformation projectionInformation = factory.getProjectionInformation(leafType);
			filteredProperties.add(propertyPath);
			if (projectionInformation.isClosed()) {
				String propertyDotPath = propertyPath.toDotPath();
				for (PropertyDescriptor secondInputProperty : projectionInformation.getInputProperties()) {
					String source = propertyDotPath + "." + secondInputProperty.getName();
					PropertyPath nestedPropertyPath = PropertyPath.from(source, type);
					filteredProperties.add(nestedPropertyPath);
					addPropertiesFromProjection(factory, type, filteredProperties, source, mappingContext);
				}
			} else {
				PropertyPath nestedPropertyPath = PropertyPath.from(inputProperty, type);
				filteredProperties.add(nestedPropertyPath);
			}
		}
	}

	private static void addPropertiesFromProjection(Neo4jPersistentEntity<?> persistentEntity, Collection<PropertyPath> filteredProperties, String inputProperty, Neo4jMappingContext mappingContext) {
		PropertyPath propertyPath = PropertyPath.from(inputProperty, persistentEntity.getTypeInformation());
		if (filteredProperties.contains(propertyPath)) {
			return;
		}
		Class<?> leafType = propertyPath.getLeafType();
		if (Neo4jSimpleTypes.HOLDER.isSimpleType(leafType) || mappingContext.hasCustomWriteTarget(leafType)) {
			filteredProperties.add(propertyPath);
		} else if (mappingContext.hasPersistentEntityFor(leafType)) {
			addPropertiesFromEntity(filteredProperties, propertyPath, leafType, mappingContext);
		}
	}

	private static void addPropertiesFromEntity(Collection<PropertyPath> filteredProperties, PropertyPath propertyPath, Class<?> leafType, Neo4jMappingContext mappingContext) {
		Neo4jPersistentEntity<?> persistetEntityFromProperty = mappingContext.getPersistentEntity(leafType);
		List<String> propertyNames = persistetEntityFromProperty.getGraphProperties().stream().map(GraphPropertyDescription::getFieldName).collect(Collectors.toList());
		filteredProperties.add(propertyPath);
		for (String propertyName : propertyNames) {
			addPropertiesFromProjection(persistetEntityFromProperty, filteredProperties, propertyName, mappingContext);
		}
		propertyNames = persistetEntityFromProperty.getRelationships().stream().map(RelationshipDescription::getFieldName).collect(Collectors.toList());
		for (String propertyName : propertyNames) {
			addPropertiesFromProjection(persistetEntityFromProperty, filteredProperties, propertyName, mappingContext);
		}
	}
}
