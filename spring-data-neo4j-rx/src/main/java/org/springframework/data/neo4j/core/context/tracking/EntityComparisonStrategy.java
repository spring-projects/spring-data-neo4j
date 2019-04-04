/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.context.tracking;

import static org.springframework.data.neo4j.core.context.tracking.EntityTrackingStrategy.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.PropertyDescription;

/**
 * Dirty tracking strategy based on property comparison. The strategy can compare two states of the same instance on
 * attribute level but uses hashes for collection-like fields. Only fields that are considered to be node properties
 * will get compared.
 *
 * @author Gerrit Meier
 */
public class EntityComparisonStrategy implements EntityTrackingStrategy {

	private final Map<Integer, EntityState> statesOfEntities = new HashMap<>();

	@Override
	public void track(NodeDescription nodeDescription, Object entity) {
		statesOfEntities.put(getObjectIdentifier(entity),
				new EntityState(nodeDescription, entity, getDefaultObjectIdentifier()));
	}

	@Override
	public void untrack(Object entity) {
		statesOfEntities.remove(getObjectIdentifier(entity));
	}

	@Override
	public Collection<EntityChangeEvent> getAggregatedEntityChangeEvents(Object entity) {
		int objectIdentifier = getObjectIdentifier(entity);
		return statesOfEntities.get(objectIdentifier).aggregateChanges(entity);
	}

	/**
	 * Compares two entities of the same instance and creates a delta {@link EntityChangeEvent}.
	 */
	private static class EntityState {

		private final Map<String, Object> oldState;
		private final List<Field> objectFields;
		private final Object identifier;
		private final Function<Object, Integer> objectIdentifyingFuction;

		EntityState(NodeDescription nodeDescription, Object entity, Function<Object, Integer> objectIdentifyingFuction) {

			Collection<PropertyDescription> properties = nodeDescription.getProperties();
			this.objectFields = getFieldsFromProperties(entity, properties);

			this.objectIdentifyingFuction = objectIdentifyingFuction;
			this.identifier = this.objectIdentifyingFuction.apply(entity);
			this.oldState = retrieveStateFrom(entity);
		}

		Set<EntityChangeEvent> aggregateChanges(Object newObject) {

			if (!sameObject(objectIdentifyingFuction.apply(newObject))) {
				throw new IllegalArgumentException("The objects to compare are not the same.");
			}
			Set<EntityChangeEvent> changes = new HashSet<>();

			for (Field field : objectFields) {
				Object newValue = getFieldValueOrHashCode(field, newObject);
				Object oldValue = oldState.get(field.getName());

				// check oldValue null first to avoid NPE in `equals` calls
				if (oldValue == null && newValue == null) {
					continue;
				}
				if (oldValue == null || !oldValue.equals(newValue)) {
					changes.add(new EntityChangeEvent(field.getName(), newValue));
				}
			}

			return changes;
		}

		private List<Field> getFieldsFromProperties(Object entity, Collection<PropertyDescription> properties) {

			List<Field> fields = new ArrayList<>();

			Class<?> entityClass = entity.getClass();
			Map<String, Field> classFields = retrieveAllFields(entityClass);
			for (PropertyDescription property : properties) {
				fields.add(classFields.get(property.getFieldName()));
			}

			return fields;
		}

		private Map<String, Field> retrieveAllFields(Class<?> entityClass) {

			Map<String, Field> classFields = new HashMap<>();

			Class<?> classToScan = entityClass;

			do {
				for (Field field : classToScan.getDeclaredFields()) {
					classFields.put(field.getName(), field);
				}
				classToScan = classToScan.getSuperclass();
			} while (classToScan != null);

			return classFields;
		}

		private boolean sameObject(Integer objectIdentifier) {
			return this.identifier.equals(objectIdentifier);
		}

		private Map<String, Object> retrieveStateFrom(Object entity) {

			Map<String, Object> state = new HashMap<>();

			for (Field field : objectFields) {
				state.put(field.getName(), getFieldValueOrHashCode(field, entity));
			}

			return Collections.unmodifiableMap(state);
		}

		private Object getFieldValueOrHashCode(Field field, Object entity) {
			try {
				field.setAccessible(true);
				Object value = field.get(entity);
				field.setAccessible(false);
				if (value == null) {
					return null;
				} else if (!Collection.class.isAssignableFrom(value.getClass())) {
					return value;
				} else {
					return value.hashCode();
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Cannot determine field value", e);
			}
		}
	}
}
