/*
 * Copyright 2011-2024 the original author or authors.
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
import org.springframework.data.neo4j.core.mapping.GraphPropertyDescription;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * This class is responsible for creating a List of {@link PropertyPath} entries that contains all reachable
 * properties (w/o circles).
 */
@API(status = API.Status.INTERNAL, since = "6.1.3")
public final class PropertyFilterSupport {

	public static Collection<PropertyFilter.ProjectedPath> getInputProperties(ResultProcessor resultProcessor, ProjectionFactory factory,
																			  Neo4jMappingContext mappingContext) {

		ReturnedType returnedType = resultProcessor.getReturnedType();
		Class<?> potentiallyProjectedType = returnedType.getReturnedType();
		Class<?> domainType = returnedType.getDomainType();

		Collection<PropertyFilter.ProjectedPath> filteredProperties = new HashSet<>();

		boolean isProjecting = returnedType.isProjecting();
		boolean isClosedProjection = factory.getProjectionInformation(potentiallyProjectedType).isClosed();

		if (!isProjecting || !isClosedProjection) {
			return Collections.emptySet();
		}

		for (String inputProperty : returnedType.getInputProperties()) {
			addPropertiesFrom(domainType, potentiallyProjectedType, factory,
					filteredProperties, new ProjectionPathProcessor(inputProperty, PropertyPath.from(inputProperty, potentiallyProjectedType).getLeafProperty().getTypeInformation()), mappingContext);
		}
		for (String inputProperty : KPropertyFilterSupport.getRequiredProperties(domainType)) {
			addPropertiesFrom(domainType, potentiallyProjectedType, factory,
					filteredProperties, new ProjectionPathProcessor(inputProperty, PropertyPath.from(inputProperty, domainType).getLeafProperty().getTypeInformation()), mappingContext);
		}

		return filteredProperties;
	}

	static Collection<PropertyFilter.ProjectedPath> addPropertiesFrom(Class<?> domainType, Class<?> returnType,
																			  ProjectionFactory projectionFactory,
																			  Neo4jMappingContext neo4jMappingContext) {

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnType);
		Collection<PropertyFilter.ProjectedPath> propertyPaths = new HashSet<>();
		Neo4jPersistentEntity<?> domainEntity = neo4jMappingContext.getRequiredPersistentEntity(domainType);

		for (PropertyDescriptor inputProperty : projectionInformation.getInputProperties()) {
			TypeInformation<?> typeInformation = null;
			if (projectionInformation.isClosed()) {
				typeInformation = PropertyPath.from(inputProperty.getName(), returnType).getTypeInformation();
			} else {
				// try to figure out the right property by name
				for (GraphPropertyDescription graphProperty : domainEntity.getGraphProperties()) {
					if (graphProperty.getPropertyName().equals(inputProperty.getName())) {
						typeInformation = domainEntity.getPersistentProperty(graphProperty.getFieldName()).getTypeInformation();
						break;
					}
				}
				// it could still be null for relationships
				if (typeInformation == null) {
					for (RelationshipDescription relationshipDescription : domainEntity.getRelationships()) {
						if (relationshipDescription.getFieldName().equals(inputProperty.getName())) {
							typeInformation = domainEntity.getPersistentProperty(relationshipDescription.getFieldName()).getTypeInformation();
							break;
						}
					}
				}
			}
			addPropertiesFrom(domainType, returnType, projectionFactory, propertyPaths, new ProjectionPathProcessor(inputProperty.getName(), typeInformation), neo4jMappingContext);
		}
		return propertyPaths;
	}

	private static void addPropertiesFrom(Class<?> domainType, Class<?> returnedType, ProjectionFactory factory,
										  Collection<PropertyFilter.ProjectedPath> filteredProperties, ProjectionPathProcessor projectionPathProcessor,
										  Neo4jMappingContext mappingContext) {

		ProjectionInformation projectionInformation = factory.getProjectionInformation(returnedType);
		PropertyFilter.RelaxedPropertyPath propertyPath;

		// If this is a closed projection we can assume that the return type (possible projection type) contains
		// only fields accessible with a property path.
		if (projectionInformation.isClosed()) {
			propertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(returnedType).append(projectionPathProcessor.path);
		} else {
			// otherwise the domain type is used right from the start
			propertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(domainType).append(projectionPathProcessor.path);
		}

		Class<?> propertyType = projectionPathProcessor.typeInformation.getType();
		TypeInformation<?> currentTypeInformation = projectionPathProcessor.typeInformation.getActualType();
		if (projectionPathProcessor.typeInformation.isMap()) {
			// deep inspection into the map to look for the related entity type.
			TypeInformation<?> mapValueType = projectionPathProcessor.typeInformation.getRequiredMapValueType();
			if (mapValueType.isCollectionLike()) {
				currentTypeInformation = projectionPathProcessor.typeInformation.getRequiredMapValueType().getComponentType();
				propertyType = projectionPathProcessor.typeInformation.getRequiredMapValueType().getComponentType().getType();
			} else {
				currentTypeInformation = projectionPathProcessor.typeInformation.getRequiredMapValueType();
				propertyType = projectionPathProcessor.typeInformation.getRequiredMapValueType().getType();
			}
		} else if (projectionPathProcessor.typeInformation.isCollectionLike()) {
			currentTypeInformation = projectionPathProcessor.typeInformation.getComponentType();
			propertyType = projectionPathProcessor.typeInformation.getComponentType().getType();
		}

		// 1. Simple types can be added directly
		// 2. Something that looks like an entity needs to get processed as such
		// 3. Embedded projection
		if (mappingContext.getConversionService().isSimpleType(propertyType)) {
			filteredProperties.add(new PropertyFilter.ProjectedPath(propertyPath, false));
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			filteredProperties.add(new PropertyFilter.ProjectedPath(propertyPath, true));
		} else {
			ProjectionInformation nestedProjectionInformation = factory.getProjectionInformation(propertyType);
			// Closed projection should get handled as above (recursion)
			if (nestedProjectionInformation.isClosed()) {
				filteredProperties.add(new PropertyFilter.ProjectedPath(propertyPath, false));
				for (PropertyDescriptor nestedInputProperty : nestedProjectionInformation.getInputProperties()) {
					TypeInformation<?> typeInformation = currentTypeInformation.getProperty(nestedInputProperty.getName());
					ProjectionPathProcessor nextProjectionPathProcessor = projectionPathProcessor.next(nestedInputProperty, typeInformation);

					if (projectionPathProcessor.isChildLevel() &&
							(domainType.equals(nextProjectionPathProcessor.typeInformation.getType())
							|| returnedType.equals(nextProjectionPathProcessor.typeInformation.getActualType().getType())
							|| returnedType.equals(nextProjectionPathProcessor.typeInformation.getType()))) {
						break;
					}

					addPropertiesFrom(domainType, returnedType, factory, filteredProperties,
							nextProjectionPathProcessor, mappingContext);
				}
			} else {
				// An open projection at this place needs to get replaced with the matching (real) entity
				// Use domain type as root type for the property path
				PropertyFilter.RelaxedPropertyPath domainBasedPropertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(domainType).append(projectionPathProcessor.path);
				filteredProperties.add(new PropertyFilter.ProjectedPath(domainBasedPropertyPath, true));
			}
		}
	}

	private static class ProjectionPathProcessor {
		final TypeInformation<?> typeInformation;
		final String path;
		final String name;

		private ProjectionPathProcessor(String name, String path, @Nullable TypeInformation<?> typeInformation) {
			this.typeInformation = typeInformation;
			this.path = path;
			this.name = name;
		}

		private ProjectionPathProcessor(String name, @Nullable TypeInformation<?> typeInformation) {
			this(name, name, typeInformation);
		}

		public ProjectionPathProcessor next(PropertyDescriptor nextProperty, TypeInformation<?> nextTypeInformation) {
			String nextPropertyName = nextProperty.getName();
			return new ProjectionPathProcessor(nextPropertyName, path + "." + nextPropertyName, nextTypeInformation);
		}

		public boolean isChildLevel() {
			return path.contains(".");
		}
	}

}
