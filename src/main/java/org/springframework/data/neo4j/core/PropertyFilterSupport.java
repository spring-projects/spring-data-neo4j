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
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
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

/**
 * This class is responsible for creating a List of {@link PropertyPath} entries that contains all reachable
 * properties (w/o circles).
 */
public class PropertyFilterSupport {

	public static List<PropertyPath> getInputProperties(ResultProcessor resultProcessor, ProjectionFactory factory,
														Neo4jMappingContext mappingContext) {

		ReturnedType returnedType = resultProcessor.getReturnedType();
		List<PropertyPath> filteredProperties = new ArrayList<>();

		boolean isProjecting = returnedType.isProjecting();
		for (String inputProperty : returnedType.getInputProperties()) {
			if (isProjecting) {
				addPropertiesFrom(returnedType.getDomainType(), returnedType.getReturnedType(), factory,
						filteredProperties, inputProperty, mappingContext);
			} else {
				addPropertiesFromEntity(filteredProperties, PropertyPath.from(inputProperty, returnedType.getDomainType()),
						returnedType.getReturnedType(), mappingContext, new HashSet<>());
			}
		}
		return isProjecting ? filteredProperties : Collections.emptyList();
	}

	public static List<PropertyPath> addPropertiesFrom(Class<?> returnType, Class<?> domainType,
													   ProjectionFactory projectionFactory,
													   Neo4jMappingContext neo4jMappingContext) {

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnType);
		List<PropertyPath> propertyPaths = new ArrayList<>();
		for (PropertyDescriptor inputProperty : projectionInformation.getInputProperties()) {
			addPropertiesFrom(returnType, domainType, projectionFactory, propertyPaths, inputProperty.getName(), neo4jMappingContext);
		}
		return propertyPaths;
	}

	private static void addPropertiesFrom(Class<?> domainType, Class<?> returnedType, ProjectionFactory factory,
										 Collection<PropertyPath> filteredProperties, String inputProperty,
										 Neo4jMappingContext mappingContext) {

		ProjectionInformation projectionInformation = factory.getProjectionInformation(returnedType);
		PropertyPath propertyPath;

		// If this is a closed projection we can assume that the return type (possible projection type) contains
		// only fields accessible with a property path.
		if (projectionInformation.isClosed()) {
			propertyPath = PropertyPath.from(inputProperty, returnedType);
		} else {
			// otherwise the domain type is used right from the start
			propertyPath = PropertyPath.from(inputProperty, domainType);
		}

		Class<?> propertyType = propertyPath.getLeafType();
		// 1. Simple types can be added directly
		// 2. Something that looks like an entity needs to get processed as such
		// 3. Embedded projection
		if (mappingContext.getConversionService().isSimpleType(propertyType)) {
			filteredProperties.add(propertyPath);
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			// avoid recursion / cycles
			if (propertyType.equals(domainType)) {
				return;
			}
			processEntity(domainType, filteredProperties, inputProperty, mappingContext);

		} else {
			ProjectionInformation nestedProjectionInformation = factory.getProjectionInformation(propertyType);
			filteredProperties.add(propertyPath);
			// Closed projection should get handled as above (recursion)
			if (nestedProjectionInformation.isClosed()) {
				for (PropertyDescriptor nestedInputProperty : nestedProjectionInformation.getInputProperties()) {
					PropertyPath nestedPropertyPath = propertyPath.nested(nestedInputProperty.getName());
					filteredProperties.add(nestedPropertyPath);
					addPropertiesFrom(domainType, returnedType, factory, filteredProperties,
							nestedPropertyPath.toDotPath(), mappingContext);
				}
			} else {
				// an open projection at this place needs to get replaced with the matching (real) entity
				processEntity(domainType, filteredProperties, inputProperty, mappingContext);
			}
		}
	}

	private static void processEntity(Class<?> domainType, Collection<PropertyPath> filteredProperties,
									  String inputProperty, Neo4jMappingContext mappingContext) {

		Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainType);
		Neo4jPersistentProperty persistentProperty = persistentEntity.getPersistentProperty(inputProperty);
		Class<?> propertyEntityType = persistentProperty.getActualType();

		// Use domain type as root type for the property path
		PropertyPath propertyPath = PropertyPath.from(inputProperty, domainType);
		addPropertiesFromEntity(filteredProperties, propertyPath, propertyEntityType, mappingContext, new HashSet<>());
	}

	private static void addPropertiesFromEntity(Collection<PropertyPath> filteredProperties, PropertyPath propertyPath,
												Class<?> propertyType, Neo4jMappingContext mappingContext,
												Collection<Neo4jPersistentEntity<?>> processedEntities) {

		Neo4jPersistentEntity<?> persistentEntityFromProperty = mappingContext.getPersistentEntity(propertyType);
		// break the recursion / cycles
		if (hasProcessedEntity(persistentEntityFromProperty, processedEntities)) {
			return;
		}
		processedEntities.add(persistentEntityFromProperty);

		// save base/root entity/projection type to avoid recursion later
		Class<?> pathRootType = propertyPath.getOwningType().getType();
		if (mappingContext.hasPersistentEntityFor(pathRootType)) {
			processedEntities.add(mappingContext.getPersistentEntity(pathRootType));
		}

		takeAllPropertiesFromEntity(filteredProperties, propertyPath, mappingContext, persistentEntityFromProperty, processedEntities);
	}

	private static boolean hasProcessedEntity(Neo4jPersistentEntity<?> persistentEntityFromProperty,
											  Collection<Neo4jPersistentEntity<?>> processedEntities) {

		return processedEntities.contains(persistentEntityFromProperty);
	}

	private static void takeAllPropertiesFromEntity(Collection<PropertyPath> filteredProperties,
													PropertyPath propertyPath, Neo4jMappingContext mappingContext,
													Neo4jPersistentEntity<?> persistentEntityFromProperty,
													Collection<Neo4jPersistentEntity<?>> processedEntities) {

		filteredProperties.add(propertyPath);

		persistentEntityFromProperty.doWithAll(neo4jPersistentProperty -> {
			addPropertiesFrom(filteredProperties, propertyPath.nested(neo4jPersistentProperty.getFieldName()), mappingContext, processedEntities);
		});
	}

	private static void addPropertiesFrom(Collection<PropertyPath> filteredProperties, PropertyPath propertyPath,
										  Neo4jMappingContext mappingContext,
										  Collection<Neo4jPersistentEntity<?>> processedEntities) {

		// break the recursion / cycles
		if (filteredProperties.contains(propertyPath)) {
			return;
		}
		Class<?> propertyType = propertyPath.getLeafType();
		// simple types can get added directly to the list.
		if (mappingContext.getConversionService().isSimpleType(propertyType)) {
			filteredProperties.add(propertyPath);
		// Other types are handled also as entities because there cannot be any nested projection within a real entity.
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			addPropertiesFromEntity(filteredProperties, propertyPath, propertyType, mappingContext, processedEntities);
		}
	}
}
