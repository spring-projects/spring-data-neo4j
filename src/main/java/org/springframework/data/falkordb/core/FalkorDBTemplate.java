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

package org.springframework.data.falkordb.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apiguardian.api.API;

import org.springframework.data.domain.Sort;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBPersistentEntity;
import org.springframework.data.falkordb.core.mapping.FalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.util.Assert;

/**
 * Primary implementation of {@link FalkorDBOperations}. This class provides object
 * mapping between Java entities and FalkorDB graph structures.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public class FalkorDBTemplate implements FalkorDBOperations {

	private final FalkorDBClient falkorDBClient;

	private final FalkorDBMappingContext mappingContext;

	private final FalkorDBEntityConverter entityConverter;

	public FalkorDBTemplate(FalkorDBClient falkorDBClient, FalkorDBMappingContext mappingContext,
			FalkorDBEntityConverter entityConverter) {
		Assert.notNull(falkorDBClient, "FalkorDBClient must not be null");
		Assert.notNull(mappingContext, "FalkorDBMappingContext must not be null");
		Assert.notNull(entityConverter, "FalkorDBEntityConverter must not be null");

		this.falkorDBClient = falkorDBClient;
		this.mappingContext = mappingContext;

		// If the entity converter is DefaultFalkorDBEntityConverter and doesn't have a
		// client,
		// create a new one with the client for relationship support
		if (entityConverter instanceof DefaultFalkorDBEntityConverter) {
			DefaultFalkorDBEntityConverter defaultConverter = (DefaultFalkorDBEntityConverter) entityConverter;
			// Create a new converter with the FalkorDB client for relationship loading
			this.entityConverter = new DefaultFalkorDBEntityConverter(mappingContext,
					new org.springframework.data.mapping.model.EntityInstantiators(), falkorDBClient);
		}
		else {
			this.entityConverter = entityConverter;
		}
	}

	@Override
	public <T> T save(T instance) {
		Assert.notNull(instance, "Entity to save must not be null");

		Class<?> entityType = instance.getClass();
		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext
			.getRequiredPersistentEntity(entityType);

		String primaryLabel = getPrimaryLabel(persistentEntity);

		// Simple save implementation - would need to be enhanced with proper ID handling
		Map<String, Object> properties = new HashMap<>();
		this.entityConverter.write(instance, properties);

		// Generate Cypher for saving
		StringBuilder cypher = new StringBuilder("CREATE (n:");
		cypher.append(primaryLabel);
		cypher.append(" ");

		// Separate regular properties from interned properties
		Map<String, Object> regularParams = new HashMap<>();
		List<String> propertyAssignments = new ArrayList<>();
		
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			
			if (value instanceof DefaultFalkorDBEntityConverter.InternedValue) {
				// For interned values, inline the intern() function call
				DefaultFalkorDBEntityConverter.InternedValue internedValue = 
					(DefaultFalkorDBEntityConverter.InternedValue) value;
				propertyAssignments.add(key + ": intern('" + internedValue.getValue() + "')");
			} else {
				// For regular values, use parameters
				propertyAssignments.add(key + ": $" + key);
				regularParams.put(key, value);
			}
		}

		// Add properties
		if (!propertyAssignments.isEmpty()) {
			cypher.append("{ ");
			cypher.append(String.join(", ", propertyAssignments));
			cypher.append(" }");
		}

		cypher.append(") RETURN n, id(n) as nodeId");

		return this.falkorDBClient.query(cypher.toString(), regularParams, result -> {
			// Convert back to entity
			for (FalkorDBClient.Record record : result.records()) {
				T savedEntity = (T) this.entityConverter.read(entityType, record);

				// Handle relationships after saving the main entity
				Object entityId = record.get("nodeId"); // Get the internal FalkorDB ID
				// Handle relationships after saving the main entity
				if (entityId != null && this.entityConverter instanceof DefaultFalkorDBEntityConverter) {
					((DefaultFalkorDBEntityConverter) this.entityConverter).saveRelationships(instance, entityId);
				}
				// entityConverter).saveRelationships(instance, entityId);
				// }

				return savedEntity;
			}
			return null;
		});
	}

	@Override
	public <T> List<T> saveAll(Iterable<T> instances) {
		List<T> savedEntities = new ArrayList<>();
		for (T instance : instances) {
			savedEntities.add(save(instance));
		}
		return savedEntities;
	}

	@Override
	public <T> Optional<T> findById(Object id, Class<T> clazz) {
		Assert.notNull(id, "ID must not be null");
		Assert.notNull(clazz, "Class must not be null");

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		String cypher = "MATCH (n:" + primaryLabel + ") WHERE id(n) = $id RETURN n";
		Map<String, Object> parameters = Collections.singletonMap("id", id);

		return this.falkorDBClient.query(cypher, parameters, result -> {
			for (FalkorDBClient.Record record : result.records()) {
				return Optional.of(this.entityConverter.read(clazz, record));
			}
			return Optional.empty();
		});
	}

	@Override
	public <T> List<T> findAllById(Iterable<?> ids, Class<T> clazz) {
		Assert.notNull(ids, "IDs must not be null");
		Assert.notNull(clazz, "Class must not be null");

		List<Object> idList = new ArrayList<>();
		ids.forEach(idList::add);

		if (idList.isEmpty()) {
			return Collections.emptyList();
		}

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		String cypher = "MATCH (n:" + primaryLabel + ") WHERE id(n) IN $ids RETURN n";
		Map<String, Object> parameters = Collections.singletonMap("ids", idList);

		return this.falkorDBClient.query(cypher, parameters, result -> {
			List<T> entities = new ArrayList<>();
			for (FalkorDBClient.Record record : result.records()) {
				entities.add(this.entityConverter.read(clazz, record));
			}
			return entities;
		});
	}

	@Override
	public <T> List<T> findAll(Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		String cypher = "MATCH (n:" + primaryLabel + ") RETURN n";

		return this.falkorDBClient.query(cypher, Collections.emptyMap(), result -> {
			List<T> entities = new ArrayList<>();
			for (FalkorDBClient.Record record : result.records()) {
				entities.add(this.entityConverter.read(clazz, record));
			}
			return entities;
		});
	}

	@Override
	public <T> List<T> findAll(Class<T> clazz, Sort sort) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(sort, "Sort must not be null");

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		StringBuilder cypher = new StringBuilder("MATCH (n:" + primaryLabel + ") RETURN n");

		if (sort.isSorted()) {
			cypher.append(" ORDER BY ");
			String orderBy = sort.stream()
				.map(order -> "n." + order.getProperty() + " " + order.getDirection().name())
				.collect(Collectors.joining(", "));
			cypher.append(orderBy);
		}

		return this.falkorDBClient.query(cypher.toString(), Collections.emptyMap(), result -> {
			List<T> entities = new ArrayList<>();
			for (FalkorDBClient.Record record : result.records()) {
				entities.add(this.entityConverter.read(clazz, record));
			}
			return entities;
		});
	}

	@Override
	public <T> long count(Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		String cypher = "MATCH (n:" + primaryLabel + ") RETURN count(n) as count";

		return this.falkorDBClient.query(cypher, Collections.emptyMap(), result -> {
			for (FalkorDBClient.Record record : result.records()) {
				Object count = record.get("count");
				return (count instanceof Number) ? ((Number) count).longValue() : 0L;
			}
			return 0L;
		});
	}

	@Override
	public <T> boolean existsById(Object id, Class<T> clazz) {
		Assert.notNull(id, "ID must not be null");
		Assert.notNull(clazz, "Class must not be null");

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		String cypher = "MATCH (n:" + primaryLabel + ") WHERE id(n) = $id RETURN count(n) > 0 as exists";
		Map<String, Object> parameters = Collections.singletonMap("id", id);

		return this.falkorDBClient.query(cypher, parameters, result -> {
			for (FalkorDBClient.Record record : result.records()) {
				Object exists = record.get("exists");
				return (exists instanceof Boolean) ? (Boolean) exists : false;
			}
			return false;
		});
	}

	@Override
	public <T> void deleteById(Object id, Class<T> clazz) {
		Assert.notNull(id, "ID must not be null");
		Assert.notNull(clazz, "Class must not be null");

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		String cypher = "MATCH (n:" + primaryLabel + ") WHERE id(n) = $id DELETE n";
		Map<String, Object> parameters = Collections.singletonMap("id", id);

		this.falkorDBClient.query(cypher, parameters);
	}

	@Override
	public <T> void deleteAllById(Iterable<?> ids, Class<T> clazz) {
		Assert.notNull(ids, "IDs must not be null");
		Assert.notNull(clazz, "Class must not be null");

		List<Object> idList = new ArrayList<>();
		ids.forEach(idList::add);

		if (idList.isEmpty()) {
			return;
		}

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		String cypher = "MATCH (n:" + primaryLabel + ") WHERE id(n) IN $ids DELETE n";
		Map<String, Object> parameters = Collections.singletonMap("ids", idList);

		this.falkorDBClient.query(cypher, parameters);
	}

	@Override
	public <T> void deleteAll(Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		DefaultFalkorDBPersistentEntity<?> persistentEntity = this.mappingContext.getRequiredPersistentEntity(clazz);
		String primaryLabel = getPrimaryLabel(persistentEntity);

		String cypher = "MATCH (n:" + primaryLabel + ") DELETE n";

		this.falkorDBClient.query(cypher, Collections.emptyMap());
	}

	@Override
	public <T> List<T> query(String cypher, Map<String, Object> parameters, Class<T> clazz) {
		Assert.hasText(cypher, "Cypher query must not be null or empty");
		Assert.notNull(parameters, "Parameters must not be null");
		Assert.notNull(clazz, "Class must not be null");

		return this.falkorDBClient.query(cypher, parameters, result -> {
			List<T> entities = new ArrayList<>();
			for (FalkorDBClient.Record record : result.records()) {
				entities.add(this.entityConverter.read(clazz, record));
			}
			return entities;
		});
	}

	@Override
	public <T> Optional<T> queryForObject(String cypher, Map<String, Object> parameters, Class<T> clazz) {
		Assert.hasText(cypher, "Cypher query must not be null or empty");
		Assert.notNull(parameters, "Parameters must not be null");
		Assert.notNull(clazz, "Class must not be null");

		return this.falkorDBClient.query(cypher, parameters, result -> {
			for (FalkorDBClient.Record record : result.records()) {
				return Optional.of(this.entityConverter.read(clazz, record));
			}
			return Optional.empty();
		});
	}

	@Override
	public <T> T query(String cypher, Map<String, Object> parameters,
					   java.util.function.Function<FalkorDBClient.QueryResult, T> resultMapper) {
		Assert.hasText(cypher, "Cypher query must not be null or empty");
		Assert.notNull(parameters, "Parameters must not be null");
		Assert.notNull(resultMapper, "Result mapper must not be null");

		return this.falkorDBClient.query(cypher, parameters, resultMapper);
	}

	/**
	 * Returns the {@link FalkorDBEntityConverter} used by this template.
	 *
	 * @return the entity converter
	 */
	public FalkorDBEntityConverter getConverter() {
		return this.entityConverter;
	}

	/**
	 * Returns the {@link FalkorDBMappingContext} used by this template.
	 *
	 * @return the mapping context
	 */
	public FalkorDBMappingContext getMappingContext() {
		return this.mappingContext;
	}

	private String getPrimaryLabel(DefaultFalkorDBPersistentEntity<?> persistentEntity) {
		// Get the primary label from the @Node annotation
		Node nodeAnnotation = persistentEntity.getType().getAnnotation(Node.class);
		if (nodeAnnotation != null) {
			if (!nodeAnnotation.primaryLabel().isEmpty()) {
				return nodeAnnotation.primaryLabel();
			}
			else if (nodeAnnotation.labels().length > 0) {
				return nodeAnnotation.labels()[0];
			}
		}

		// Fallback to class name
		return persistentEntity.getType().getSimpleName();
	}

}
