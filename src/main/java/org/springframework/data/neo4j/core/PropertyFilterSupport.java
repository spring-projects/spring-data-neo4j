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

import org.apiguardian.api.API;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * This class is responsible for creating a List of {@link PropertyPath} entries that contains all reachable
 * properties (w/o circles).
 */
@API(status = API.Status.INTERNAL, since = "6.1.3")
public final class PropertyFilterSupport {

	public static Map<PropertyPath, Boolean> getInputProperties(ResultProcessor resultProcessor, ProjectionFactory factory,
													   Neo4jMappingContext mappingContext) {

		ReturnedType returnedType = resultProcessor.getReturnedType();
		Map<PropertyPath, Boolean> filteredProperties = new HashMap<>();

		boolean isProjecting = returnedType.isProjecting();
		boolean isClosedProjection = factory.getProjectionInformation(returnedType.getReturnedType()).isClosed();

		if (!isProjecting || !isClosedProjection) {
			return Collections.emptyMap();
		}

		for (String inputProperty : returnedType.getInputProperties()) {
			addPropertiesFrom(returnedType.getDomainType(), returnedType.getReturnedType(), factory,
					filteredProperties, inputProperty, mappingContext);
		}

		return filteredProperties;
	}

	static Map<PropertyPath, Boolean> addPropertiesFrom(Class<?> domainType, Class<?> returnType,
													   ProjectionFactory projectionFactory,
													   Neo4jMappingContext neo4jMappingContext) {

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnType);
		Map<PropertyPath, Boolean> propertyPaths = new HashMap<>();
		for (PropertyDescriptor inputProperty : projectionInformation.getInputProperties()) {
			addPropertiesFrom(domainType, returnType, projectionFactory, propertyPaths, inputProperty.getName(), neo4jMappingContext);
		}
		return propertyPaths;
	}

	private static void addPropertiesFrom(Class<?> domainType, Class<?> returnedType, ProjectionFactory factory,
										  Map<PropertyPath, Boolean> filteredProperties, String inputProperty,
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
			filteredProperties.put(propertyPath, false);
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			addPropertiesFromEntity(filteredProperties, propertyPath, propertyType, mappingContext, new HashSet<>());
		} else {
			ProjectionInformation nestedProjectionInformation = factory.getProjectionInformation(propertyType);
			// Closed projection should get handled as above (recursion)
			if (nestedProjectionInformation.isClosed()) {
				filteredProperties.put(propertyPath, false);
				for (PropertyDescriptor nestedInputProperty : nestedProjectionInformation.getInputProperties()) {
					PropertyPath nestedPropertyPath = propertyPath.nested(nestedInputProperty.getName());
					if (propertyPath.hasNext() && (domainType.equals(propertyPath.getLeafProperty().getOwningType().getType())
					|| returnedType.equals(propertyPath.getLeafProperty().getOwningType().getType()))) {
						break;
					}

					addPropertiesFrom(domainType, returnedType, factory, filteredProperties,
							nestedPropertyPath.toDotPath(), mappingContext);
				}
			} else {
				// an open projection at this place needs to get replaced with the matching (real) entity
				filteredProperties.put(propertyPath, true);
				processEntity(domainType, filteredProperties, inputProperty, mappingContext);
			}
		}
	}

	private static void processEntity(Class<?> domainType, Map<PropertyPath, Boolean> filteredProperties,
									  String inputProperty, Neo4jMappingContext mappingContext) {

		Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(domainType);
		Neo4jPersistentProperty persistentProperty = persistentEntity.getPersistentProperty(inputProperty);
		Class<?> propertyEntityType = persistentProperty.getActualType();

		// Use domain type as root type for the property path
		PropertyPath propertyPath = PropertyPath.from(inputProperty, domainType);
		addPropertiesFromEntity(filteredProperties, propertyPath, propertyEntityType, mappingContext, new HashSet<>());
	}

	private static void addPropertiesFromEntity(Map<PropertyPath, Boolean> filteredProperties, PropertyPath propertyPath,
												Class<?> propertyType, Neo4jMappingContext mappingContext,
												Collection<Neo4jPersistentEntity<?>> processedEntities) {

		if (!mappingContext.hasPersistentEntityFor(propertyType)) {
			throw new RuntimeException("hmmmm");
		}

		Neo4jPersistentEntity<?> persistentEntityFromProperty = mappingContext.getPersistentEntity(propertyType);
		// break the recursion / cycles
		if (hasProcessedEntity(persistentEntityFromProperty, processedEntities)) {
			return;
		}

		filteredProperties.put(propertyPath, true);

	}

	private static boolean hasProcessedEntity(Neo4jPersistentEntity<?> persistentEntityFromProperty,
											  Collection<Neo4jPersistentEntity<?>> processedEntities) {

		return processedEntities.contains(persistentEntityFromProperty);
	}
}
