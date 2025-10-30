/*
 * Copyright 2011-2025 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.apiguardian.api.API;

import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.neo4j.core.mapping.GraphPropertyDescription;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;

/**
 * This class is responsible for creating a List of {@link PropertyPath} entries that
 * contains all reachable properties (w/o circles).
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
@API(status = API.Status.INTERNAL, since = "6.1.3")
public final class PropertyFilterSupport {

	// A cache to look up if there are aggregate boundaries between two entities.
	private static final AggregateBoundaries AGGREGATE_BOUNDARIES = new AggregateBoundaries();

	private PropertyFilterSupport() {
	}

	public static Collection<PropertyFilter.ProjectedPath> getInputProperties(ResultProcessor resultProcessor,
			ProjectionFactory factory, Neo4jMappingContext mappingContext) {

		ReturnedType returnedType = resultProcessor.getReturnedType();
		Class<?> potentiallyProjectedType = returnedType.getReturnedType();
		Class<?> domainType = returnedType.getDomainType();

		Collection<PropertyFilter.ProjectedPath> filteredProperties = new HashSet<>();

		boolean isProjecting = returnedType.isProjecting();
		boolean isClosedProjection = factory.getProjectionInformation(potentiallyProjectedType).isClosed();
		if (!isProjecting && containsAggregateBoundary(domainType, mappingContext)) {
			Collection<PropertyFilter.ProjectedPath> listForAggregate = createListForAggregate(domainType,
					mappingContext);
			return listForAggregate;
		}

		if (!isProjecting || !isClosedProjection) {
			return Collections.emptySet();
		}

		for (String inputProperty : returnedType.getInputProperties()) {
			addPropertiesFrom(domainType, potentiallyProjectedType, factory, filteredProperties,
					new ProjectionPathProcessor(inputProperty,
							PropertyPath.from(inputProperty, potentiallyProjectedType)
								.getLeafProperty()
								.getTypeInformation()),
					mappingContext);
		}
		for (String inputProperty : KPropertyFilterSupport.getRequiredProperties(domainType)) {
			addPropertiesFrom(domainType, potentiallyProjectedType, factory, filteredProperties,
					new ProjectionPathProcessor(inputProperty,
							PropertyPath.from(inputProperty, domainType).getLeafProperty().getTypeInformation()),
					mappingContext);
		}

		return filteredProperties;
	}

	public static Collection<PropertyFilter.ProjectedPath> getInputPropertiesForAggregateBoundary(Class<?> domainType,
			Neo4jMappingContext mappingContext) {
		if (!containsAggregateBoundary(domainType, mappingContext)) {
			return Collections.emptySet();
		}
		Collection<PropertyFilter.ProjectedPath> listForAggregate = createListForAggregate(domainType, mappingContext);
		return listForAggregate;
	}

	public static Predicate<PropertyFilter.RelaxedPropertyPath> createRelaxedPropertyPathFilter(Class<?> domainType,
			Neo4jMappingContext mappingContext) {
		if (!containsAggregateBoundary(domainType, mappingContext)) {
			return PropertyFilter.NO_FILTER;
		}
		Collection<PropertyFilter.RelaxedPropertyPath> relaxedPropertyPathFilter = createRelaxedPropertyPathFilter(
				domainType, mappingContext, new HashSet<RelationshipDescription>());
		return (rpp) -> {
			return relaxedPropertyPathFilter.contains(rpp);
		};
	}

	private static Collection<PropertyFilter.RelaxedPropertyPath> createRelaxedPropertyPathFilter(Class<?> domainType,
			Neo4jMappingContext neo4jMappingContext, Set<RelationshipDescription> processedRelationships) {
		var relaxedPropertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(domainType);
		var relaxedPropertyPaths = new ArrayList<PropertyFilter.RelaxedPropertyPath>();
		relaxedPropertyPaths.add(relaxedPropertyPath);
		Neo4jPersistentEntity<?> domainEntity = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		domainEntity.getGraphProperties().stream().forEach(property -> {
			relaxedPropertyPaths.add(relaxedPropertyPath.append(property.getFieldName()));
		});
		for (RelationshipDescription relationshipDescription : domainEntity.getRelationshipsInHierarchy(any -> true)) {
			var target = relationshipDescription.getTarget();
			PropertyFilter.RelaxedPropertyPath relationshipPath = relaxedPropertyPath
				.append(relationshipDescription.getFieldName());
			relaxedPropertyPaths.add(relationshipPath);
			processedRelationships.add(relationshipDescription);
			createRelaxedPropertyPathFilter(domainType, target, relationshipPath, relaxedPropertyPaths,
					processedRelationships);
		}
		return relaxedPropertyPaths;
	}

	private static Collection<PropertyFilter.RelaxedPropertyPath> createRelaxedPropertyPathFilter(Class<?> domainType,
			NodeDescription<?> nodeDescription, PropertyFilter.RelaxedPropertyPath relaxedPropertyPath,
			Collection<PropertyFilter.RelaxedPropertyPath> relaxedPropertyPaths,
			Set<RelationshipDescription> processedRelationships) {
		// always add the related entity itself
		relaxedPropertyPaths.add(relaxedPropertyPath);
		if (nodeDescription.hasAggregateBoundaries(domainType)) {
			relaxedPropertyPaths.add(relaxedPropertyPath
				.append(((Neo4jPersistentEntity<?>) nodeDescription).getRequiredIdProperty().getFieldName()));

			return relaxedPropertyPaths;
		}
		nodeDescription.getGraphProperties().stream().forEach(property -> {
			relaxedPropertyPaths.add(relaxedPropertyPath.append(property.getFieldName()));
		});
		for (RelationshipDescription relationshipDescription : nodeDescription
			.getRelationshipsInHierarchy(any -> true)) {
			if (processedRelationships.contains(relationshipDescription)) {
				continue;
			}
			var target = relationshipDescription.getTarget();
			PropertyFilter.RelaxedPropertyPath relationshipPath = relaxedPropertyPath
				.append(relationshipDescription.getFieldName());
			relaxedPropertyPaths.add(relationshipPath);
			processedRelationships.add(relationshipDescription);
			createRelaxedPropertyPathFilter(domainType, target, relationshipPath, relaxedPropertyPaths,
					processedRelationships);
		}
		return relaxedPropertyPaths;
	}

	private static Collection<PropertyFilter.ProjectedPath> createListForAggregate(Class<?> domainType,
			Neo4jMappingContext neo4jMappingContext) {
		var relaxedPropertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(domainType);
		var filteredProperties = new ArrayList<PropertyFilter.ProjectedPath>();
		Neo4jPersistentEntity<?> domainEntity = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		domainEntity.getGraphProperties().stream().forEach(property -> {
			filteredProperties
				.add(new PropertyFilter.ProjectedPath(relaxedPropertyPath.append(property.getFieldName()), false));
		});
		for (RelationshipDescription relationshipDescription : domainEntity.getRelationshipsInHierarchy(any -> true)) {
			var target = relationshipDescription.getTarget();
			filteredProperties.addAll(createListForAggregate(domainType, target,
					relaxedPropertyPath.append(relationshipDescription.getFieldName())));
		}
		return filteredProperties;
	}

	private static Collection<PropertyFilter.ProjectedPath> createListForAggregate(Class<?> domainType,
			NodeDescription<?> nodeDescription, PropertyFilter.RelaxedPropertyPath relaxedPropertyPath) {
		var filteredProperties = new ArrayList<PropertyFilter.ProjectedPath>();
		// always add the related entity itself
		filteredProperties.add(new PropertyFilter.ProjectedPath(relaxedPropertyPath, false));
		if (nodeDescription.hasAggregateBoundaries(domainType)) {
			filteredProperties.add(new PropertyFilter.ProjectedPath(
					relaxedPropertyPath
						.append(((Neo4jPersistentEntity<?>) nodeDescription).getRequiredIdProperty().getFieldName()),
					false));
			return filteredProperties;
		}
		nodeDescription.getGraphProperties().stream().forEach(property -> {
			filteredProperties
				.add(new PropertyFilter.ProjectedPath(relaxedPropertyPath.append(property.getFieldName()), false));
		});
		for (RelationshipDescription relationshipDescription : nodeDescription
			.getRelationshipsInHierarchy(any -> true)) {
			var target = relationshipDescription.getTarget();
			filteredProperties.addAll(createListForAggregate(domainType, target,
					relaxedPropertyPath.append(relationshipDescription.getFieldName())));
		}
		return filteredProperties;
	}

	private static boolean containsAggregateBoundary(Class<?> domainType, Neo4jMappingContext neo4jMappingContext) {
		var processedRelationships = new HashSet<RelationshipDescription>();
		Neo4jPersistentEntity<?> domainEntity = neo4jMappingContext.getRequiredPersistentEntity(domainType);
		if (AGGREGATE_BOUNDARIES.hasEntry(domainEntity, domainType)) {
			return AGGREGATE_BOUNDARIES.getCachedStatus(domainEntity, domainType);
		}
		for (RelationshipDescription relationshipDescription : domainEntity.getRelationshipsInHierarchy(any -> true)) {
			var target = relationshipDescription.getTarget();
			if (target.hasAggregateBoundaries(domainType)) {
				AGGREGATE_BOUNDARIES.add(domainEntity, domainType, true);
				return true;
			}
			processedRelationships.add(relationshipDescription);
			boolean containsAggregateBoundary = containsAggregateBoundary(domainType, target, processedRelationships);
			AGGREGATE_BOUNDARIES.add(domainEntity, domainType, containsAggregateBoundary);
			return containsAggregateBoundary;
		}
		AGGREGATE_BOUNDARIES.add(domainEntity, domainType, false);
		return false;
	}

	private static boolean containsAggregateBoundary(Class<?> domainType, NodeDescription<?> nodeDescription,
			Set<RelationshipDescription> processedRelationships) {
		for (RelationshipDescription relationshipDescription : nodeDescription
			.getRelationshipsInHierarchy(any -> true)) {
			var target = relationshipDescription.getTarget();
			Class<?> underlyingClass = nodeDescription.getUnderlyingClass();
			if (processedRelationships.contains(relationshipDescription)) {
				continue;
			}
			if (target.hasAggregateBoundaries(domainType)) {
				return true;
			}
			processedRelationships.add(relationshipDescription);
			return containsAggregateBoundary(domainType, target, processedRelationships);
		}
		return false;
	}

	static Collection<PropertyFilter.ProjectedPath> addPropertiesFrom(Class<?> domainType, Class<?> returnType,
			ProjectionFactory projectionFactory, Neo4jMappingContext neo4jMappingContext) {

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnType);
		Collection<PropertyFilter.ProjectedPath> propertyPaths = new HashSet<>();
		Neo4jPersistentEntity<?> domainEntity = neo4jMappingContext.getRequiredPersistentEntity(domainType);

		for (PropertyDescriptor inputProperty : projectionInformation.getInputProperties()) {
			TypeInformation<?> typeInformation = null;
			if (projectionInformation.isClosed()) {
				typeInformation = PropertyPath.from(inputProperty.getName(), returnType).getTypeInformation();
			}
			else {
				// try to figure out the right property by name
				for (GraphPropertyDescription graphProperty : domainEntity.getGraphProperties()) {
					if (graphProperty.getPropertyName().equals(inputProperty.getName())) {
						typeInformation = Optional
							.ofNullable(domainEntity.getPersistentProperty(graphProperty.getFieldName()))
							.map(PersistentProperty::getTypeInformation)
							.orElse(null);
						break;
					}
				}
				// it could still be null for relationships
				if (typeInformation == null) {
					for (RelationshipDescription relationshipDescription : domainEntity.getRelationships()) {
						if (relationshipDescription.getFieldName().equals(inputProperty.getName())) {
							typeInformation = Optional
								.ofNullable(domainEntity.getPersistentProperty(relationshipDescription.getFieldName()))
								.map(PersistentProperty::getTypeInformation)
								.orElse(null);
							break;
						}
					}
				}
			}
			if (typeInformation != null) {
				addPropertiesFrom(domainType, returnType, projectionFactory, propertyPaths,
						new ProjectionPathProcessor(inputProperty.getName(), typeInformation), neo4jMappingContext);
			}
		}
		return propertyPaths;
	}

	private static void addPropertiesFrom(Class<?> domainType, Class<?> returnedType, ProjectionFactory factory,
			Collection<PropertyFilter.ProjectedPath> filteredProperties,
			ProjectionPathProcessor projectionPathProcessor, Neo4jMappingContext mappingContext) {

		ProjectionInformation projectionInformation = factory.getProjectionInformation(returnedType);
		PropertyFilter.RelaxedPropertyPath propertyPath;

		// If this is a closed projection we can assume that the return type (possible
		// projection type) contains
		// only fields accessible with a property path.
		if (projectionInformation.isClosed()) {
			propertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(returnedType)
				.append(projectionPathProcessor.path);
		}
		else {
			// otherwise the domain type is used right from the start
			propertyPath = PropertyFilter.RelaxedPropertyPath.withRootType(domainType)
				.append(projectionPathProcessor.path);
		}

		Class<?> propertyType = projectionPathProcessor.typeInformation.getType();
		TypeInformation<?> currentTypeInformation = projectionPathProcessor.typeInformation.getActualType();
		if (projectionPathProcessor.typeInformation.isMap()) {
			// deep inspection into the map to look for the related entity type.
			TypeInformation<?> mapValueType = projectionPathProcessor.typeInformation.getRequiredMapValueType();
			if (mapValueType.isCollectionLike()) {
				currentTypeInformation = projectionPathProcessor.typeInformation.getRequiredMapValueType()
					.getComponentType();
				propertyType = Objects.requireNonNull(currentTypeInformation, "Cannot retrieve collection type")
					.getType();
			}
			else {
				currentTypeInformation = projectionPathProcessor.typeInformation.getRequiredMapValueType();
				propertyType = currentTypeInformation.getType();
			}
		}
		else if (projectionPathProcessor.typeInformation.isCollectionLike()) {
			currentTypeInformation = projectionPathProcessor.typeInformation.getComponentType();
			propertyType = Objects.requireNonNull(currentTypeInformation, "Cannot retrieve collection type").getType();
		}

		Objects.requireNonNull(currentTypeInformation, "Property type is required");

		// 1. Simple types can be added directly
		// 2. Something that looks like an entity needs to get processed as such
		// 3. Embedded projection
		if (mappingContext.getConversionService().isSimpleType(propertyType)) {
			filteredProperties.add(new PropertyFilter.ProjectedPath(propertyPath, false));
		}
		else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			filteredProperties.add(new PropertyFilter.ProjectedPath(propertyPath, true));
		}
		else {
			ProjectionInformation nestedProjectionInformation = factory.getProjectionInformation(propertyType);
			// Closed projection should get handled as above (recursion)
			if (nestedProjectionInformation.isClosed()) {
				filteredProperties.add(new PropertyFilter.ProjectedPath(propertyPath, false));
				for (PropertyDescriptor nestedInputProperty : nestedProjectionInformation.getInputProperties()) {
					TypeInformation<?> typeInformation = currentTypeInformation
						.getRequiredProperty(nestedInputProperty.getName());
					ProjectionPathProcessor nextProjectionPathProcessor = projectionPathProcessor
						.next(nestedInputProperty, typeInformation);

					TypeInformation<?> actualType = Objects
						.requireNonNull(nextProjectionPathProcessor.typeInformation.getActualType());
					if (projectionPathProcessor.isChildLevel()
							&& (domainType.equals(nextProjectionPathProcessor.typeInformation.getType())
									|| returnedType.equals(actualType.getType())
									|| returnedType.equals(nextProjectionPathProcessor.typeInformation.getType()))) {
						break;
					}

					if (projectionPathProcessor.typeInformation.getActualType() != null
							&& projectionPathProcessor.typeInformation.getActualType()
								.getType()
								.equals(actualType.getType())
							|| (!projectionPathProcessor.typeInformation.isCollectionLike()
									&& !projectionPathProcessor.typeInformation.isMap()
									&& projectionPathProcessor.typeInformation.getType()
										.equals(nextProjectionPathProcessor.typeInformation.getType()))) {
						filteredProperties.add(new PropertyFilter.ProjectedPath(propertyPath, true));
					}
					else {
						addPropertiesFrom(domainType, returnedType, factory, filteredProperties,
								nextProjectionPathProcessor, mappingContext);
					}
				}
			}
			else {
				// An open projection at this place needs to get replaced with the
				// matching (real) entity
				// Use domain type as root type for the property path
				PropertyFilter.RelaxedPropertyPath domainBasedPropertyPath = PropertyFilter.RelaxedPropertyPath
					.withRootType(domainType)
					.append(projectionPathProcessor.path);
				filteredProperties.add(new PropertyFilter.ProjectedPath(domainBasedPropertyPath, true));
			}
		}
	}

	private static final class ProjectionPathProcessor {

		final TypeInformation<?> typeInformation;

		final String path;

		final String name;

		private ProjectionPathProcessor(String name, String path, TypeInformation<?> typeInformation) {
			this.typeInformation = typeInformation;
			this.path = path;
			this.name = name;
		}

		private ProjectionPathProcessor(String name, TypeInformation<?> typeInformation) {
			this(name, name, typeInformation);
		}

		ProjectionPathProcessor next(PropertyDescriptor nextProperty, TypeInformation<?> nextTypeInformation) {
			String nextPropertyName = nextProperty.getName();
			return new ProjectionPathProcessor(nextPropertyName, this.path + "." + nextPropertyName,
					nextTypeInformation);
		}

		boolean isChildLevel() {
			return this.path.contains(".");
		}

	}

	record AggregateBoundary(Neo4jPersistentEntity<?> entity, Class<?> domainType, boolean status) {

	}

	private static final class AggregateBoundaries {

		private final Set<AggregateBoundary> aggregateBoundaries = new HashSet<>();

		private final ReentrantLock lock = new ReentrantLock();

		void add(Neo4jPersistentEntity<?> entity, Class<?> domainType, boolean status) {
			try {
				this.lock.lock();
				for (AggregateBoundary aggregateBoundary : this.aggregateBoundaries) {
					if (aggregateBoundary.domainType().equals(domainType) && aggregateBoundary.entity().equals(entity)
							&& aggregateBoundary.status() != status) {
						throw new IllegalStateException("%s cannot have a different status to %s. Was %s now %s"
							.formatted(entity.getUnderlyingClass(), domainType, aggregateBoundary.status(), status));
					}
				}
				this.aggregateBoundaries.add(new AggregateBoundary(entity, domainType, status));
			}
			finally {
				this.lock.unlock();
			}
		}

		boolean hasEntry(Neo4jPersistentEntity<?> entity, Class<?> domainType) {
			try {
				this.lock.lock();
				for (AggregateBoundary aggregateBoundary : this.aggregateBoundaries) {
					if (aggregateBoundary.domainType().equals(domainType)
							&& aggregateBoundary.entity().equals(entity)) {
						return true;
					}
				}
				return false;
			}
			finally {
				this.lock.unlock();
			}
		}

		boolean getCachedStatus(Neo4jPersistentEntity<?> entity, Class<?> domainType) {
			try {
				this.lock.lock();
				for (AggregateBoundary aggregateBoundary : this.aggregateBoundaries) {
					if (aggregateBoundary.domainType().equals(domainType)
							&& aggregateBoundary.entity().equals(entity)) {
						return aggregateBoundary.status();
					}
				}
				return false;
			}
			finally {
				this.lock.unlock();
			}
		}

	}

}
