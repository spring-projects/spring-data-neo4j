/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.mapping;

import static java.util.Collections.*;

import java.lang.reflect.Field;

import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.neo4j.ogm.metadata.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * This class implements Spring Data's MappingContext interface, scavenging the required data from the OGM's metadata in
 * order to for SDN to play nicely with Spring Data REST. The main thing to note is that this class is effectively a
 * container shim for {@code ClassInfo} objects. We don't reload all the mapping information again.
 *
 * @author Vince Bickers
 * @author Adam George
 * @author Mark Paluch
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
		metaData.persistentEntities().stream().filter(k -> k.getUnderlyingClass() != null)
				.forEach(k -> addPersistentEntity(k.getUnderlyingClass()));
		logger.info("Neo4jMappingContext initialisation completed");
	}

	@Override
	protected <T> Neo4jPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		logger.debug("Creating Neo4jPersistentEntity from type information: {}", typeInformation);
		return new Neo4jPersistentEntity<>(typeInformation);
	}

	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property, Neo4jPersistentEntity<?> owner,
			SimpleTypeHolder simpleTypeHolder) {

		ClassInfo owningClassInfo = this.metaData.classInfo(owner.getType().getName());

		Field propertyField = property.getField().orElse(null);

		if (!property.isFieldBacked() && owningClassInfo != null) {
			FieldInfo fieldInfo = owningClassInfo.propertyFieldByName(property.getName());
			if (fieldInfo == null) {
				fieldInfo = owningClassInfo.relationshipFieldByName(property.getName());
			}
			if (fieldInfo != null) {
				propertyField = owningClassInfo.getField(fieldInfo);
			} else {
				// there is no field, probably because descriptor gave us a field name derived from a getter
				logger.debug("Couldn't resolve a concrete field corresponding to property {} on {} ", property.getName(),
						owningClassInfo.name());
			}
		}

		return new Neo4jPersistentProperty(owningClassInfo, property, owner,
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
