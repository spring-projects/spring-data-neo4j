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
package org.springframework.data.falkordb.core.mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.schema.Relationship;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link FalkorDBEntityConverter}.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class DefaultFalkorDBEntityConverter implements FalkorDBEntityConverter {

	private final FalkorDBMappingContext mappingContext;

	private final EntityInstantiators entityInstantiators;

	private FalkorDBClient falkorDBClient; // Optional for relationship loading

	/**
	 * Creates a new {@link DefaultFalkorDBEntityConverter}.
	 * @param mappingContext must not be {@literal null}.
	 * @param entityInstantiators must not be {@literal null}.
	 */
	public DefaultFalkorDBEntityConverter(FalkorDBMappingContext mappingContext,
			EntityInstantiators entityInstantiators) {
		Assert.notNull(mappingContext, "MappingContext must not be null");
		Assert.notNull(entityInstantiators, "EntityInstantiators must not be null");

		this.mappingContext = mappingContext;
		this.entityInstantiators = entityInstantiators;
	}

	/**
	 * Creates a new {@link DefaultFalkorDBEntityConverter} with FalkorDB client for
	 * relationship loading.
	 * @param mappingContext must not be {@literal null}.
	 * @param entityInstantiators must not be {@literal null}.
	 * @param falkorDBClient the client for executing relationship queries, may be
	 * {@literal null}.
	 */
	public DefaultFalkorDBEntityConverter(FalkorDBMappingContext mappingContext,
			EntityInstantiators entityInstantiators, FalkorDBClient falkorDBClient) {
		this(mappingContext, entityInstantiators);
		this.falkorDBClient = falkorDBClient;
	}

	@Override
	public <R> R read(Class<R> type, FalkorDBClient.Record record) {
		if (record == null) {
			return null;
		}

		// Handle primitive/wrapper types directly
		if (isPrimitiveOrWrapperType(type)) {
			return readPrimitiveValue(type, record);
		}

		TypeInformation<? extends R> typeInfo = TypeInformation.of(type);
		FalkorDBPersistentEntity<R> entity = (FalkorDBPersistentEntity<R>) this.mappingContext
			.getRequiredPersistentEntity(typeInfo);

		return read(typeInfo, record, entity);
	}

	private <R> R read(TypeInformation<? extends R> type, FalkorDBClient.Record record,
			FalkorDBPersistentEntity<R> entity) {

		// Create parameter value provider for constructor parameters
		ParameterValueProvider<FalkorDBPersistentProperty> parameterProvider = new FalkorDBParameterValueProvider(
				record, (DefaultFalkorDBPersistentEntity<?>) entity);

		// Get entity instantiator and create instance
		EntityInstantiator instantiator = this.entityInstantiators.getInstantiatorFor(entity);
		R instance = instantiator.createInstance(entity, parameterProvider);

		// Set properties on the created instance
		PersistentPropertyAccessor<R> accessor = entity.getPropertyAccessor(instance);

		entity.doWithProperties((FalkorDBPersistentProperty property) -> {
			if (entity.isCreatorArgument(property)) {
				// Skip properties that were set via constructor
				return;
			}

			if (property.isRelationship()) {
				// Handle relationship loading
				Object relationshipValue = loadRelationship(record, property, entity);
				if (relationshipValue != null) {
					accessor.setProperty(property, relationshipValue);
				}
				return;
			}

			// Get property value from record
			Object value = getValueFromRecord(record, property);
			if (value != null) {
				// Convert value to the correct type
				Object convertedValue = convertValueFromFalkorDB(value, property.getType());
				accessor.setProperty(property, convertedValue);
			}
		});

		return instance;
	}

	@Override
	public void write(Object source, Map<String, Object> sink) {
		if (source == null) {
			return;
		}

		FalkorDBPersistentEntity<?> entity = this.mappingContext.getRequiredPersistentEntity(source.getClass());
		PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);

		entity.doWithProperties((FalkorDBPersistentProperty property) -> {
			if (property.isIdProperty() && property.isInternalIdProperty()) {
				// Skip internal IDs as they're managed by FalkorDB
				return;
			}

			if (property.isRelationship()) {
				// Relationships are handled during save in a separate phase
				// We don't include them in the node properties
				return;
			}

			Object value = accessor.getProperty(property);
			if (value != null) {
				// Convert value to FalkorDB-compatible format
				Object convertedValue = convertValueForFalkorDB(value);
				sink.put(property.getGraphPropertyName(), convertedValue);
			}
		});
	}

	/**
	 * Convert a value to a format compatible with FalkorDB. This handles special types
	 * like LocalDateTime that need formatting.
	 * @param value the value to convert
	 * @return the converted value compatible with FalkorDB
	 */
	private Object convertValueForFalkorDB(Object value) {
		if (value instanceof LocalDateTime) {
			// Convert LocalDateTime to ISO string format that FalkorDB can handle
			// Using ISO_LOCAL_DATE_TIME format: "2025-10-14T16:56:15.926694"
			return ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		}
		// Add more type conversions as needed
		return value;
	}

	/**
	 * Check if the given type is a primitive type or its wrapper.
	 * @param type the type to check
	 * @return true if the type is a primitive or wrapper type
	 */
	private boolean isPrimitiveOrWrapperType(Class<?> type) {
		return type.isPrimitive() || type == Boolean.class || type == Byte.class || type == Character.class
				|| type == Short.class || type == Integer.class || type == Long.class || type == Float.class
				|| type == Double.class || type == String.class;
	}

	/**
	 * Read a primitive value directly from the record.
	 * @param <R> the target type for conversion
	 * @param type the target primitive/wrapper type
	 * @param record the FalkorDB record
	 * @return the value converted to the target type
	 */
	@SuppressWarnings("unchecked")
	private <R> R readPrimitiveValue(Class<R> type, FalkorDBClient.Record record) {
		// For aggregate queries like count(), the result is usually in the first column
		// Try common column names first, then fall back to the first available value
		Object value = null;

		// Try common aggregate column names
		String[] commonNames = { "count", "sum", "avg", "min", "max", "result" };
		for (String name : commonNames) {
			try {
				value = record.get(name);
				if (value != null) {
					break;
				}
			}
			catch (Exception ex) {
				// Column doesn't exist, try next
			}
		}

		// If no common names found, get the first available value
		if (value == null) {
			try {
				// Get all keys and try the first one
				for (String key : record.keys()) {
					value = record.get(key);
					if (value != null) {
						break;
					}
				}
			}
			catch (Exception ex) {
				// Fallback: return null or default value
				return null;
			}
		}

		if (value == null) {
			return null;
		}

		// Convert the value to the target type
		return (R) convertValueFromFalkorDB(value, type);
	}

	/**
	 * Convert a value from FalkorDB to the target type. This handles type mismatches like
	 * Long to Integer.
	 * @param value the value from FalkorDB
	 * @param targetType the target type to convert to
	 * @return the converted value
	 */
	private Object convertValueFromFalkorDB(Object value, Class<?> targetType) {
		if (value == null) {
			return null;
		}

		// If the value is already of the target type, return as is
		if (targetType.isInstance(value)) {
			return value;
		}

		// Handle numeric conversions
		if (value instanceof Number) {
			Number numValue = (Number) value;
			if (targetType == Long.class || targetType == long.class) {
				return numValue.longValue();
			}
			else if (targetType == Integer.class || targetType == int.class) {
				return numValue.intValue();
			}
			else if (targetType == Double.class || targetType == double.class) {
				return numValue.doubleValue();
			}
			else if (targetType == Float.class || targetType == float.class) {
				return numValue.floatValue();
			}
			else if (targetType == Short.class || targetType == short.class) {
				return numValue.shortValue();
			}
			else if (targetType == Byte.class || targetType == byte.class) {
				return numValue.byteValue();
			}
		}

		// Handle String conversions
		if (value instanceof String) {
			String strValue = (String) value;
			if (targetType == LocalDateTime.class) {
				try {
					return LocalDateTime.parse(strValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				}
				catch (Exception ex) {
					// If parsing fails, return null
					return null;
				}
			}
			// For String target type, return as is
			if (targetType == String.class) {
				return strValue;
			}
		}

		// Handle Boolean conversions
		if (value instanceof Boolean && (targetType == Boolean.class || targetType == boolean.class)) {
			return value;
		}

		// Fallback: return the original value
		return value;
	}

	private Object getValueFromRecord(FalkorDBClient.Record record, FalkorDBPersistentProperty property) {
		try {
			String propertyName = property.getGraphPropertyName();

			// Handle ID property specially - it comes from nodeId or internal id
			if (property.isIdProperty()) {
				// Try to get nodeId first (internal FalkorDB ID)
				Object nodeId = record.get("nodeId");
				if (nodeId != null) {
					return nodeId;
				}
				// Fallback to id field
				Object id = record.get("id");
				if (id != null) {
					return id;
				}
			}

			// For regular properties, try to get from node object first
			Object nodeValue = extractValueFromNodeObject(record, propertyName);
			if (nodeValue != null) {
				return nodeValue;
			}

			// Fallback: try direct property name access
			return record.get(propertyName);
		}
		catch (Exception ex) {
			// Property might not exist in record
			return null;
		}
	}

	/**
	 * Load a relationship property from the record. Executes Cypher queries to load
	 * related entities based on relationship configuration.
	 * @param record the FalkorDB record
	 * @param property the relationship property
	 * @param entity the entity metadata
	 * @return the loaded relationship value or null
	 */
	private Object loadRelationship(FalkorDBClient.Record record, FalkorDBPersistentProperty property,
			FalkorDBPersistentEntity<?> entity) {
		Relationship relationshipAnnotation = property.findAnnotation(Relationship.class);
		if (relationshipAnnotation == null) {
			return null;
		}

		String relationshipType = relationshipAnnotation.value();
		if (relationshipType.isEmpty()) {
			// Use property name as relationship type if not specified
			relationshipType = property.getName().toUpperCase();
		}

		// Get the source node ID from the current record
		Object sourceNodeId = getNodeIdFromRecord(record);
		if (sourceNodeId == null) {
			return null;
		}

		// Determine target type
		Class<?> targetType = getRelationshipTargetType(property);
		if (targetType == null) {
			return null;
		}

		// Get target entity metadata
		FalkorDBPersistentEntity<?> targetEntity = this.mappingContext.getRequiredPersistentEntity(targetType);
		String targetLabel = getPrimaryLabel(targetEntity);

		// Build Cypher query based on relationship direction
		String cypher = buildRelationshipQuery(relationshipAnnotation.direction(), relationshipType, targetLabel);

		// Create parameters for the query
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("sourceId", sourceNodeId);

		// Execute query and convert results
		if (isCollectionProperty(property)) {
			return loadRelationshipCollection(cypher, parameters, targetType, property);
		}
		else {
			return loadSingleRelationship(cypher, parameters, targetType);
		}
	}

	/**
	 * Save relationships for an entity. This would be called after saving the main
	 * entity.
	 * @param entity the entity whose relationships to save
	 * @param id the ID of the entity
	 */
	public void saveRelationships(Object entity, Object id) {
		FalkorDBPersistentEntity<?> persistentEntity = this.mappingContext
			.getRequiredPersistentEntity(entity.getClass());
		PersistentPropertyAccessor<Object> accessor = persistentEntity.getPropertyAccessor(entity);

		for (FalkorDBPersistentProperty property : persistentEntity) {
			if (property.isRelationship()) {
				Object relationshipValue = accessor.getProperty(property);
				if (relationshipValue != null) {
					saveRelationship(id, property, relationshipValue);
				}
			}
		}
	}

	/**
	 * Save a single relationship.
	 * @param sourceId the ID of the source entity
	 * @param property the relationship property
	 * @param relationshipValue the value of the relationship
	 */
	private void saveRelationship(Object sourceId, FalkorDBPersistentProperty property, Object relationshipValue) {
		if (this.falkorDBClient == null) {
			return; // No client available for relationship saving
		}

		Relationship relationshipAnnotation = property.findAnnotation(Relationship.class);
		if (relationshipAnnotation == null) {
			return;
		}

		String relationshipType = relationshipAnnotation.value();
		if (relationshipType.isEmpty()) {
			relationshipType = property.getName().toUpperCase();
		}

		// Handle collections vs single relationships
		if (isCollectionProperty(property)) {
			saveRelationshipCollection(sourceId, relationshipAnnotation, relationshipType,
					(Collection<?>) relationshipValue);
		}
		else {
			saveSingleRelationshipEntity(sourceId, relationshipAnnotation, relationshipType, relationshipValue);
		}
	}

	/**
	 * Save a collection of relationship entities.
	 * @param sourceId the ID of the source entity
	 * @param relationshipAnnotation the relationship annotation
	 * @param relationshipType the type of relationship
	 * @param relatedEntities the collection of related entities
	 */
	private void saveRelationshipCollection(Object sourceId, Relationship relationshipAnnotation,
			String relationshipType, Collection<?> relatedEntities) {
		if (relatedEntities == null || relatedEntities.isEmpty()) {
			return;
		}

		for (Object relatedEntity : relatedEntities) {
			if (relatedEntity != null) {
				saveSingleRelationshipEntity(sourceId, relationshipAnnotation, relationshipType, relatedEntity);
			}
		}
	}

	/**
	 * Save a single relationship entity.
	 * @param sourceId the ID of the source entity
	 * @param relationshipAnnotation the relationship annotation
	 * @param relationshipType the type of relationship
	 * @param relatedEntity the related entity to save
	 */
	private void saveSingleRelationshipEntity(Object sourceId, Relationship relationshipAnnotation,
			String relationshipType, Object relatedEntity) {
		try {
			// First, ensure the related entity is saved (get or create its ID)
			Object targetId = ensureEntitySaved(relatedEntity);
			if (targetId == null) {
				return; // Could not save or get ID for target entity
			}

			// Extract relationship properties if the entity has them
			Map<String, Object> relationshipProperties = extractRelationshipProperties(relatedEntity);

			// Create the relationship with or without properties
			String cypher = buildRelationshipSaveQuery(relationshipAnnotation.direction(), relationshipType,
					relationshipProperties);
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("sourceId", sourceId);
			parameters.put("targetId", targetId);

			// Add relationship properties to parameters with rel_ prefix
			if (relationshipProperties != null) {
				for (Map.Entry<String, Object> entry : relationshipProperties.entrySet()) {
					parameters.put("rel_" + entry.getKey(), entry.getValue());
				}
			}

			// Execute relationship creation query
			this.falkorDBClient.query(cypher, parameters);

		}
		catch (Exception ex) {
			// Log error - relationship save failed
			// In a production system, you might want to throw an exception or
			// handle this
			// differently
		}
	}

	/**
	 * Ensure an entity is saved and return its ID.
	 * @param entity the entity to ensure is saved
	 * @return the ID of the entity, or null if unable to determine ID
	 */
	private Object ensureEntitySaved(Object entity) {
		// This is a simplified approach - in a full implementation, you might:
		// 1. Check if the entity already has an ID
		// 2. If not, save it first
		// 3. Return the ID

		// For now, we'll assume entities are saved elsewhere or already have IDs
		// This method would need to be enhanced with proper entity saving
		// logic

		FalkorDBPersistentEntity<?> persistentEntity = this.mappingContext
			.getRequiredPersistentEntity(entity.getClass());
		FalkorDBPersistentProperty idProperty = persistentEntity.getIdProperty();

		if (idProperty != null) {
			Object id = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);
			if (id != null) {
				return id;
			}
		}

		// TODO: Save entity if it doesn't have an ID yet
		return null;
	}

	/**
	 * Extract node ID from a record.
	 * @param record the FalkorDB record
	 * @return the node ID, or null if unable to extract
	 */
	private Object getNodeIdFromRecord(FalkorDBClient.Record record) {
		try {
			// Try to get explicit ID field first
			Object explicitId = record.get("id");
			if (explicitId != null) {
				return explicitId;
			}
			// Fallback to internal node ID
			return record.get("nodeId");
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Determine the target type for a relationship property.
	 * @param property the relationship property
	 * @return the target type class
	 */
	private Class<?> getRelationshipTargetType(FalkorDBPersistentProperty property) {
		Class<?> propertyType = property.getType();

		// Handle collections
		if (Collection.class.isAssignableFrom(propertyType)) {
			return property.getComponentType();
		}

		return propertyType;
	}

	/**
	 * Check if a property represents a collection relationship.
	 * @param property the property to check
	 * @return true if the property is a collection, false otherwise
	 */
	private boolean isCollectionProperty(FalkorDBPersistentProperty property) {
		return Collection.class.isAssignableFrom(property.getType());
	}

	/**
	 * Get the primary label for an entity.
	 * @param entity the entity to get the primary label for
	 * @return the primary label string
	 */
	private String getPrimaryLabel(FalkorDBPersistentEntity<?> entity) {
		// This mirrors the logic from FalkorDBTemplate
		org.springframework.data.falkordb.core.schema.Node nodeAnnotation = entity.getType()
			.getAnnotation(org.springframework.data.falkordb.core.schema.Node.class);
		if (nodeAnnotation != null) {
			if (!nodeAnnotation.primaryLabel().isEmpty()) {
				return nodeAnnotation.primaryLabel();
			}
			else if (nodeAnnotation.labels().length > 0) {
				return nodeAnnotation.labels()[0];
			}
		}
		return entity.getType().getSimpleName();
	}

	/**
	 * Build Cypher query for relationship traversal based on direction.
	 * @param direction the direction of the relationship
	 * @param relationshipType the type of relationship
	 * @param targetLabel the target label for the relationship
	 * @return the constructed Cypher query string
	 */
	private String buildRelationshipQuery(Relationship.Direction direction, String relationshipType,
			String targetLabel) {
		StringBuilder cypher = new StringBuilder();
		cypher.append("MATCH (source) WHERE id(source) = $sourceId");

		switch (direction) {
			case OUTGOING:
				cypher.append("MATCH (source)-[:")
					.append(relationshipType)
					.append("]->(target:")
					.append(targetLabel)
					.append(")");
				break;
			case INCOMING:
				cypher.append("MATCH (source)<-[:")
					.append(relationshipType)
					.append("]-(target:")
					.append(targetLabel)
					.append(")");
				break;
			case UNDIRECTED:
				cypher.append("MATCH (source)-[:")
					.append(relationshipType)
					.append("]-(target:")
					.append(targetLabel)
					.append(")");
				break;
		}

		cypher.append(" RETURN target");
		return cypher.toString();
	}

	/**
	 * Load a single related entity.
	 * @param cypher the Cypher query to execute
	 * @param parameters the query parameters
	 * @param targetType the target type to convert to
	 * @return the loaded entity, or null if not found
	 */
	private Object loadSingleRelationship(String cypher, Map<String, Object> parameters, Class<?> targetType) {
		if (this.falkorDBClient == null) {
			// No client available for relationship loading
			return null;
		}

		try {
			return this.falkorDBClient.query(cypher, parameters, result -> {
				for (FalkorDBClient.Record record : result.records()) {
					// Get the target node from the record
					Object targetNode = record.get("target");
					if (targetNode instanceof FalkorDBClient.Record) {
						return read(targetType, (FalkorDBClient.Record) targetNode);
					}
				}
				return null;
			});
		}
		catch (Exception ex) {
			// Log error and return null for failed relationship loading
			return null;
		}
	}

	/**
	 * Load a collection of related entities.
	 * @param cypher the Cypher query to execute
	 * @param parameters the query parameters
	 * @param targetType the target type to convert to
	 * @param property the property being loaded
	 * @return the list of loaded entities
	 */
	private List<Object> loadRelationshipCollection(String cypher, Map<String, Object> parameters, Class<?> targetType,
			FalkorDBPersistentProperty property) {
		if (this.falkorDBClient == null) {
			return new ArrayList<>(); // No client available for relationship loading
		}

		try {
			return this.falkorDBClient.query(cypher, parameters, result -> {
				List<Object> relatedEntities = new ArrayList<>();
				for (FalkorDBClient.Record record : result.records()) {
					// Get the target node from the record
					Object targetNode = record.get("target");
					if (targetNode instanceof FalkorDBClient.Record) {
						Object entity = read(targetType, (FalkorDBClient.Record) targetNode);
						if (entity != null) {
							relatedEntities.add(entity);
						}
					}
				}
				return relatedEntities;
			});
		}
		catch (Exception ex) {
			// Log error and return empty list for failed relationship loading
			return new ArrayList<>();
		}
	}

	/**
	 * Build Cypher query for creating relationships.
	 * @param direction the direction of the relationship
	 * @param relationshipType the type of relationship
	 * @return the constructed Cypher query string
	 */
	private String buildRelationshipSaveQuery(Relationship.Direction direction, String relationshipType) {
		return buildRelationshipSaveQuery(direction, relationshipType, null);
	}

	/**
	 * Build Cypher query for creating relationships with optional properties.
	 * @param direction the direction of the relationship
	 * @param relationshipType the type of relationship
	 * @param relationshipProperties optional properties for the relationship
	 * @return the constructed Cypher query string
	 */
	private String buildRelationshipSaveQuery(Relationship.Direction direction, String relationshipType,
			Map<String, Object> relationshipProperties) {
		StringBuilder cypher = new StringBuilder();
		cypher.append("MATCH (source), (target) ").append("WHERE id(source) = $sourceId AND id(target) = $targetId ");

		// Build relationship properties if provided
		String propertiesClause = "";
		if (relationshipProperties != null && !relationshipProperties.isEmpty()) {
			StringBuilder propBuilder = new StringBuilder(" { ");
			boolean first = true;
			for (String key : relationshipProperties.keySet()) {
				if (!first) {
					propBuilder.append(", ");
				}
				propBuilder.append(key).append(": $rel_").append(key);
				first = false;
			}
			propBuilder.append(" }");
			propertiesClause = propBuilder.toString();
		}

		switch (direction) {
			case OUTGOING:
				cypher.append("MERGE (source)-[:")
					.append(relationshipType)
					.append(propertiesClause)
					.append("]->(target)");
				break;
			case INCOMING:
				cypher.append("MERGE (source)<-[:")
					.append(relationshipType)
					.append(propertiesClause)
					.append("]-(target)");
				break;
			case UNDIRECTED:
				cypher.append("MERGE (source)-[:")
					.append(relationshipType)
					.append(propertiesClause)
					.append("]-(target)");
				break;
		}

		return cypher.toString();
	}

	/**
	 * Extract relationship properties from an entity if it has @RelationshipProperties
	 * annotation.
	 * @param relatedEntity the entity to extract properties from
	 * @return a map of properties, or null if none found
	 */
	private Map<String, Object> extractRelationshipProperties(Object relatedEntity) {
		Class<?> entityClass = relatedEntity.getClass();
		if (!entityClass
			.isAnnotationPresent(org.springframework.data.falkordb.core.schema.RelationshipProperties.class)) {
			return null;
		}

		// Extract properties from the relationship entity
		Map<String, Object> properties = new HashMap<>();
		try {
			FalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(entityClass);
			for (FalkorDBPersistentProperty property : persistentEntity) {
				if (!property.isIdProperty() && !property.isRelationship()) {
					Object value = persistentEntity.getPropertyAccessor(relatedEntity).getProperty(property);
					if (value != null) {
						properties.put(property.getGraphPropertyName(), value);
					}
				}
			}
		}
		catch (Exception ex) {
			// Return empty map if extraction fails
		}

		return properties.isEmpty() ? null : properties;
	}

	/**
	 * Extract property value from node object using reflection. This method is extracted
	 * to reduce nested try depth.
	 * @param record the FalkorDB record
	 * @param propertyName the name of the property to extract
	 * @return the extracted value, or null if not found
	 */
	private Object extractValueFromNodeObject(FalkorDBClient.Record record, String propertyName) {
		try {
			Object nodeObj = record.get("n");
			if (nodeObj == null) {
				return null;
			}

			// Try getProperty method first
			Object value = tryGetPropertyMethod(nodeObj, propertyName);
			if (value != null) {
				return value;
			}

			// Try getProperties method as fallback
			return tryGetPropertiesMethod(nodeObj, propertyName);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private Object tryGetPropertyMethod(Object nodeObj, String propertyName) {
		try {
			java.lang.reflect.Method getPropertyMethod = nodeObj.getClass().getMethod("getProperty", String.class);
			Object propertyObj = getPropertyMethod.invoke(nodeObj, propertyName);
			if (propertyObj != null) {
				return extractValueFromPropertyObject(propertyObj);
			}
		}
		catch (Exception ex) {
			// Method not available or failed
		}
		return null;
	}

	private Object tryGetPropertiesMethod(Object nodeObj, String propertyName) {
		try {
			java.lang.reflect.Method getPropertiesMethod = nodeObj.getClass().getMethod("getProperties");
			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) getPropertiesMethod.invoke(nodeObj);
			Object propertyObj = properties.get(propertyName);
			if (propertyObj != null) {
				return extractValueFromPropertyObject(propertyObj);
			}
		}
		catch (Exception ex) {
			// Method not available or failed
		}
		return null;
	}

	private Object extractValueFromPropertyObject(Object propertyObj) {
		try {
			java.lang.reflect.Method getValueMethod = propertyObj.getClass().getMethod("getValue");
			return getValueMethod.invoke(propertyObj);
		}
		catch (Exception ex) {
			// Maybe the property itself is the value
			return propertyObj;
		}
	}

	/**
	 * Parameter value provider for FalkorDB records.
	 */
	private static class FalkorDBParameterValueProvider
			extends PersistentEntityParameterValueProvider<FalkorDBPersistentProperty> {

		private final FalkorDBClient.Record record;

		FalkorDBParameterValueProvider(FalkorDBClient.Record record, DefaultFalkorDBPersistentEntity<?> entity) {
			super(entity,
					new org.springframework.data.mapping.model.PropertyValueProvider<FalkorDBPersistentProperty>() {
						@Override
						public <T> T getPropertyValue(FalkorDBPersistentProperty property) {
							Object v = getValueFromRecord(record, property);
							@SuppressWarnings("unchecked")
							T cast = (T) v;
							return cast;
						}
					}, null);
			this.record = record;
		}

		private static Object getValueFromRecord(FalkorDBClient.Record record, FalkorDBPersistentProperty property) {
			try {
				String propertyName = property.getGraphPropertyName();
				return record.get(propertyName);
			}
			catch (Exception ex) {
				return null;
			}
		}

	}

}
