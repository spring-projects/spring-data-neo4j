/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.core.mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(DefaultFalkorDBEntityConverter.class);

	/**
	 * The mapping context.
	 */
	private final FalkorDBMappingContext mappingContext;

	/**
	 * The entity instantiators.
	 */
	private final EntityInstantiators entityInstantiators;

	/**
	 * Optional FalkorDB client for relationship loading.
	 */
	private FalkorDBClient falkorDBClient;

	/**
	 * Creates a new {@link DefaultFalkorDBEntityConverter}.
	 * @param context must not be {@literal null}.
	 * @param instantiators must not be {@literal null}.
	 */
	public DefaultFalkorDBEntityConverter(final FalkorDBMappingContext context,
			final EntityInstantiators instantiators) {
		Assert.notNull(context, "MappingContext must not be null");
		Assert.notNull(instantiators, "EntityInstantiators must not be null");

		this.mappingContext = context;
		this.entityInstantiators = instantiators;
	}

	/**
	 * Creates a new {@link DefaultFalkorDBEntityConverter} with FalkorDB client for
	 * relationship loading.
	 * @param mappingContext must not be {@literal null}.
	 * @param entityInstantiators must not be {@literal null}.
	 * @param client the client for executing relationship queries, may be {@literal null}.
	 */
	public DefaultFalkorDBEntityConverter(final FalkorDBMappingContext mappingContext,
			final EntityInstantiators entityInstantiators, final FalkorDBClient client) {
		this(mappingContext, entityInstantiators);
		this.falkorDBClient = client;
	}

	/**
	 * Reads a record into the specified type.
	 * @param type the target type
	 * @param record the FalkorDB record
	 * @return the converted object
	 */
	@Override
	public final <R> R read(final Class<R> type, final FalkorDBClient.Record record) {
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

	private <R> R read(final TypeInformation<? extends R> type, final FalkorDBClient.Record record,
			final FalkorDBPersistentEntity<R> entity) {

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

	/**
	 * Writes an object to a property map.
	 * @param source the source object
	 * @param sink the target property map
	 */
	@Override
	public final void write(final Object source, final Map<String, Object> sink) {
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
				Object convertedValue = convertValueForFalkorDB(value, property);
				sink.put(property.getGraphPropertyName(), convertedValue);
			}
		});
	}

	/**
	 * Convert a value to a format compatible with FalkorDB. This handles special types
	 * like LocalDateTime that need formatting and applies intern() for low-cardinality strings.
	 * @param value the value to convert
	 * @param property the property metadata (optional, can be null)
	 * @return the converted value compatible with FalkorDB
	 */
	private Object convertValueForFalkorDB(final Object value, final FalkorDBPersistentProperty property) {
		if (value instanceof LocalDateTime) {
			// Convert LocalDateTime to ISO string format that FalkorDB can handle.
			return ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		}
		if (value instanceof java.time.Instant) {
			return DateTimeFormatter.ISO_INSTANT.format((java.time.Instant) value);
		}
		
		// Apply intern() function for low-cardinality string properties
		if (property != null && property.isInterned() && value instanceof String) {
			String strValue = (String) value;
			// Escape backslashes first, then single quotes
			String escapedValue = strValue.replace("\\", "\\\\").replace("'", "\\'");
			// Return as a special marker object that will be handled during Cypher generation
			return new InternedValue(escapedValue);
		}
		
		// Add more type conversions as needed
		return value;
	}

	/**
	 * Marker class to indicate that a value should use FalkorDB's intern() function.
	 */
	public static class InternedValue {
		private final String value;

		public InternedValue(final String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	/**
	 * Check if the given type is a primitive type or its wrapper.
	 * @param type the type to check
	 * @return true if the type is a primitive or wrapper type
	 */
	private boolean isPrimitiveOrWrapperType(final Class<?> type) {
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
	private <R> R readPrimitiveValue(final Class<R> type, final FalkorDBClient.Record record) {
		// For aggregate queries like count(), the result is usually in the
		// first column. Try common column names first, then fall back to the
		// first available value
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
			else if (targetType == String.class) {
				// Convert number to string (e.g., Long ID to String)
				return numValue.toString();
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
					return null;
				}
			}
			if (targetType == java.time.Instant.class) {
				try {
					return java.time.Instant.parse(strValue);
				}
				catch (Exception ex) {
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

	private Object getValueFromRecord(final FalkorDBClient.Record record, final FalkorDBPersistentProperty property) {
		try {
			String propertyName = property.getGraphPropertyName();

			// Handle ID property specially
			if (property.isIdProperty()) {
				// 1) For user-defined IDs stored as node properties (e.g. @Id String id),
				//    try to read the value from the node object first – same strategy as
				//    for regular properties. This is the shape produced by repository
				//    queries where the node is returned as "n" and the id lives inside it.
				Object nodeIdValue = extractValueFromNodeObject(record, propertyName);
				if (nodeIdValue != null) {
					return nodeIdValue;
				}

				// 2) Fall back to explicit internal ID aliases when present. This keeps
				//    support for projections that expose id(n) AS nodeId / id.
				Object nodeId = record.get("nodeId");
				if (nodeId != null) {
					return nodeId;
				}
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
	private Object loadRelationship(final FalkorDBClient.Record record, final FalkorDBPersistentProperty property,
			final FalkorDBPersistentEntity<?> entity) {
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
			logger.warn("Failed to save relationship of type {} for entity: {}", relationshipType,
					relatedEntity.getClass().getSimpleName(), ex);
		}
	}

	/**
	 * Ensure an entity is saved and return its ID.
	 * This method implements automatic cascade save for related entities.
	 * @param entity the entity to ensure is saved
	 * @return the ID of the entity, or null if unable to save
	 */
	private Object ensureEntitySaved(Object entity) {
		if (entity == null) {
			return null;
		}

		FalkorDBPersistentEntity<?> persistentEntity = this.mappingContext
			.getRequiredPersistentEntity(entity.getClass());
		FalkorDBPersistentProperty idProperty = persistentEntity.getIdProperty();

		// Check if entity already has an ID
		if (idProperty != null) {
			Object existingId = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);
			if (existingId != null) {
				// Entity already has an ID, return it
				return existingId;
			}
		}

		// Entity doesn't have an ID yet, we need to save it
		if (this.falkorDBClient == null) {
			// No client available, cannot save
			return null;
		}

		try {
			// Save the entity and get its ID
			return saveEntityAndGetId(entity, persistentEntity);
		}
		catch (Exception ex) {
			logger.warn("Failed to cascade save entity of type {}: {}", entity.getClass().getSimpleName(),
					ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Save an entity and return its internal FalkorDB ID.
	 * This is a recursive save that handles nested relationships.
	 * @param entity the entity to save
	 * @param persistentEntity the persistent entity metadata
	 * @return the internal FalkorDB ID of the saved entity
	 */
	private Object saveEntityAndGetId(Object entity, FalkorDBPersistentEntity<?> persistentEntity) {
		String primaryLabel = getPrimaryLabel(persistentEntity);

		// Convert entity to properties (excluding relationships)
		Map<String, Object> properties = new HashMap<>();
		PersistentPropertyAccessor<?> accessor = persistentEntity.getPropertyAccessor(entity);

		persistentEntity.doWithProperties((FalkorDBPersistentProperty property) -> {
			if (property.isIdProperty() && property.isInternalIdProperty()) {
				// Skip internal IDs as they're managed by FalkorDB
				return;
			}

			if (property.isRelationship()) {
				// Skip relationships during initial entity save
				// They will be handled after the entity is saved
				return;
			}

			Object value = accessor.getProperty(property);
			if (value != null) {
				Object convertedValue = convertValueForFalkorDB(value, property);
				properties.put(property.getGraphPropertyName(), convertedValue);
			}
		});

		// Build CREATE query with all labels
		List<String> labels = new ArrayList<>();
		labels.add(primaryLabel);
		org.springframework.data.falkordb.core.schema.Node nodeAnn = persistentEntity.getType()
				.getAnnotation(org.springframework.data.falkordb.core.schema.Node.class);
		if (nodeAnn != null) {
			for (String label : nodeAnn.value()) {
				if (label != null && !label.isEmpty() && !label.equals(primaryLabel)) {
					labels.add(label);
				}
			}
			for (String label : nodeAnn.labels()) {
				if (label != null && !label.isEmpty() && !label.equals(primaryLabel)) {
					labels.add(label);
				}
			}
		}

	StringBuilder cypher = new StringBuilder("CREATE (n:");
	cypher.append(String.join(":", labels));
	cypher.append(" ");

	// Separate regular properties from interned properties
	Map<String, Object> regularParams = new HashMap<>();
	List<String> propertyAssignments = new ArrayList<>();
	
	for (Map.Entry<String, Object> entry : properties.entrySet()) {
		String key = entry.getKey();
		Object value = entry.getValue();
		
		if (value instanceof InternedValue) {
			// For interned values, inline the intern() function call
			InternedValue internedValue = (InternedValue) value;
			propertyAssignments.add(key + ": intern('" + internedValue.getValue() + "')");
		} else {
			// For regular values, use parameters
			propertyAssignments.add(key + ": $" + key);
			regularParams.put(key, value);
		}
	}

	if (!propertyAssignments.isEmpty()) {
		cypher.append("{ ");
		cypher.append(String.join(", ", propertyAssignments));
		cypher.append(" }");
	}

	cypher.append(") RETURN id(n) as nodeId");

	// Execute save and get the ID
	Object nodeId = this.falkorDBClient.query(cypher.toString(), regularParams, result -> {
			for (FalkorDBClient.Record record : result.records()) {
				return record.get("nodeId");
			}
			return null;
		});

		if (nodeId != null) {
			// Update the entity's ID property only if it's an internal FalkorDB ID
			// Never overwrite external @Id properties with internal node IDs
			FalkorDBPersistentProperty idProperty = persistentEntity.getIdProperty();
			if (idProperty != null && !idProperty.isImmutable() && idProperty.isInternalIdProperty()) {
				accessor.setProperty(idProperty, nodeId);
			}

			// Now save relationships for this entity
			saveRelationships(entity, nodeId);
		}

		return nodeId;
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
			if (nodeAnnotation.value().length > 0) {
				return nodeAnnotation.value()[0];
			}
			if (nodeAnnotation.labels().length > 0) {
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
		cypher.append("MATCH (source) WHERE id(source) = $sourceId ");

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

		// Return target node with its properties and ID
		cypher.append(" RETURN target, id(target) as targetId");
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
					// Try to read the entity directly from the record
					// The record should contain the target node's properties
					return readRelatedEntity(record, targetType);
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
					// Read the related entity from the record
					Object entity = readRelatedEntity(record, targetType);
					if (entity != null) {
						relatedEntities.add(entity);
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
	 * Read a related entity from a record.
	 * This method tries multiple strategies to extract the target node data.
	 * @param record the record containing the relationship result
	 * @param targetType the target entity type
	 * @return the converted entity, or null if extraction fails
	 */
	private Object readRelatedEntity(FalkorDBClient.Record record, Class<?> targetType) {
		try {
			// Strategy 1: Try to get target node directly
			Object targetNode = record.get("target");
			if (targetNode instanceof FalkorDBClient.Record) {
				return read(targetType, (FalkorDBClient.Record) targetNode);
			}

			// Strategy 2: Try to create a record-like structure from available data
			// Get targetId if available
			Object targetId = record.get("targetId");
			if (targetNode != null && targetId != null) {
				// Create a wrapper record that includes the node data and ID
				return readFromNodeWithId(targetNode, targetId, targetType);
			}

			// Strategy 3: If the record itself has the target properties, read directly
			if (targetNode != null) {
				return readFromNodeObject(targetNode, targetType);
			}

			return null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Read an entity from a node object that has an ID.
	 * @param nodeObj the node object
	 * @param nodeId the node ID
	 * @param targetType the target entity type
	 * @return the converted entity
	 */
	private Object readFromNodeWithId(Object nodeObj, Object nodeId, Class<?> targetType) {
		try {
			// Create a simple record wrapper that provides the necessary data
			SimpleRecord wrapperRecord = new SimpleRecord(nodeObj, nodeId);
			return read(targetType, wrapperRecord);
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Read an entity from a raw node object.
	 * @param nodeObj the node object from FalkorDB
	 * @param targetType the target entity type
	 * @return the converted entity
	 */
	private Object readFromNodeObject(Object nodeObj, Class<?> targetType) {
		try {
			// If the node object has properties, extract them
			Map<String, Object> properties = extractPropertiesFromNode(nodeObj);
			if (properties != null && !properties.isEmpty()) {
				// Create a record from the properties
				SimpleRecord record = new SimpleRecord(properties);
				return read(targetType, record);
			}
			return null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Extract properties from a node object using reflection.
	 * @param nodeObj the node object
	 * @return map of properties, or null if extraction fails
	 */
	private Map<String, Object> extractPropertiesFromNode(Object nodeObj) {
		if (nodeObj == null) {
			return null;
		}

		try {
			// Try getProperties method
			java.lang.reflect.Method getPropertiesMethod = nodeObj.getClass().getMethod("getProperties");
			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) getPropertiesMethod.invoke(nodeObj);
			if (properties != null) {
				// Convert property values if needed
				Map<String, Object> converted = new HashMap<>();
				for (Map.Entry<String, Object> entry : properties.entrySet()) {
					Object value = entry.getValue();
					// Extract value from Property wrapper if needed
					if (value != null) {
						Object extractedValue = extractValueFromPropertyObject(value);
						converted.put(entry.getKey(), extractedValue);
					}
				}
				return converted;
			}
		}
		catch (Exception ex) {
			// Method not available or failed
		}

		return null;
	}

	/**
	 * Simple record implementation for wrapping node data.
	 */
	private static class SimpleRecord implements FalkorDBClient.Record {
		private final Map<String, Object> data;
		private final Object nodeId;

		SimpleRecord(Map<String, Object> properties) {
			this.data = properties;
			this.nodeId = null;
		}

		SimpleRecord(Object nodeObj, Object nodeId) {
			this.data = new HashMap<>();
			this.nodeId = nodeId;
			// Try to extract properties from node object
			try {
				java.lang.reflect.Method getPropertiesMethod = nodeObj.getClass().getMethod("getProperties");
				@SuppressWarnings("unchecked")
				Map<String, Object> properties = (Map<String, Object>) getPropertiesMethod.invoke(nodeObj);
				if (properties != null) {
					for (Map.Entry<String, Object> entry : properties.entrySet()) {
						Object value = entry.getValue();
						if (value != null) {
							// Extract value from Property wrapper
							try {
								java.lang.reflect.Method getValueMethod = value.getClass().getMethod("getValue");
								data.put(entry.getKey(), getValueMethod.invoke(value));
							}
							catch (Exception e) {
								data.put(entry.getKey(), value);
							}
						}
					}
				}
			}
			catch (Exception ex) {
				// Failed to extract properties
			}
		}

		@Override
		public Object get(int index) {
			// Not supported for simple records
			return null;
		}

		@Override
		public Object get(String key) {
			if ("nodeId".equals(key) || "targetId".equals(key)) {
				return nodeId;
			}
			return data.get(key);
		}

		@Override
		public Iterable<String> keys() {
			return data.keySet();
		}

		@Override
		public int size() {
			return data.size();
		}

		@Override
		public Iterable<Object> values() {
			return data.values();
		}
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
