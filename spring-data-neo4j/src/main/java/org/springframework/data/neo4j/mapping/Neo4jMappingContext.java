/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.mapping;

import static java.util.Collections.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.classloader.MetaDataClassLoader;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * This class implements Spring Data's MappingContext interface, scavenging the required data from the OGM's metadata in
 * order to for SDN to play nicely with Spring Data REST. The main thing to note is that this class is effectively a
 * container shim for {@code ClassInfo} objects. We don't reload all the mapping information again.
 *
 * @author Vince Bickers
 * @author Adam George
 * @since 4.0.0
 */
public class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jMappingContext.class);

	private final MetaData metaData;

	/**
	 * Constructs a new {@link Neo4jMappingContext} based on the persistent entities in the given {@link MetaData}.
	 *
	 * @param metaData The OGM {@link MetaData} from which to extract the persistent entities
	 */
	public Neo4jMappingContext(MetaData metaData) {
		this.metaData = metaData;

		for (ClassInfo classInfo : metaData.persistentEntities()) {
			try {
				addPersistentEntity(MetaDataClassLoader.loadClass(classInfo.name()));
			} catch (ClassNotFoundException e) {
				logger.error("Failed to load class: " + classInfo.name() + " named in ClassInfo due to exception", e);
			}
		}

		logger.info("Neo4jMappingContext initialisation completed");
	}

	@Override
	protected <T> Neo4jPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		logger.debug("Creating Neo4jPersistentEntity from type information: {}", typeInformation);
		return new Neo4jPersistentEntity<>(typeInformation);
	}

	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
			Neo4jPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {

		ClassInfo owningClassInfo = this.metaData.classInfo(owner.getType().getName());

		Field propertyField = field;
		if (propertyField == null && owningClassInfo != null && descriptor != null) {
			FieldInfo fieldInfo = owningClassInfo.propertyFieldByName(descriptor.getName());
			if (fieldInfo == null) {
				fieldInfo = owningClassInfo.relationshipFieldByName(descriptor.getName());
			}
			if (fieldInfo != null) {
				propertyField = owningClassInfo.getField(fieldInfo);
			} else {
				// there is no field, probably because descriptor gave us a field name derived from a getter
				logger.debug("Couldn't resolve a concrete field corresponding to property {} on {} ", descriptor.getName(),
						owningClassInfo.name());
			}
		}

		return new Neo4jPersistentProperty(owningClassInfo, propertyField, descriptor, owner,
				updateSimpleTypeHolder(simpleTypeHolder, propertyField));
	}

	private SimpleTypeHolder updateSimpleTypeHolder(SimpleTypeHolder currentSimpleTypeHolder, Field field) {
		if (field == null) {
			return currentSimpleTypeHolder;
		}

		final Class<?> fieldType = field.getType().isArray() ? field.getType().getComponentType() : field.getType();

		if (shouldUpdateSimpleTypes(currentSimpleTypeHolder, field, fieldType)) {
			SimpleTypeHolder updatedSimpleTypeHolder = new SimpleTypeHolder(singleton(fieldType), currentSimpleTypeHolder);
			setSimpleTypeHolder(updatedSimpleTypeHolder);
			return updatedSimpleTypeHolder;
		}
		return currentSimpleTypeHolder;
	}

	private boolean shouldUpdateSimpleTypes(SimpleTypeHolder currentSimpleTypeHolder, Field field,
			Class<?> rawFieldType) {
		if (field.isAnnotationPresent(Convert.class)) {
			return true;
		}

		if (currentSimpleTypeHolder.isSimpleType(rawFieldType) || rawFieldType.isInterface()) {
			return false;
		}
		if (this.metaData.classInfo(rawFieldType.getName()) == null) {
			logger.info("No class information found in OGM meta-data for {} so treating as simple type for SD Commons",
					rawFieldType);
			return true;
		}
		return false;
	}

}
