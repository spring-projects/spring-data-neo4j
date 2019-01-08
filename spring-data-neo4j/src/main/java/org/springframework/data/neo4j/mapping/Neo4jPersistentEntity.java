/*
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty.PropertyType;
import org.springframework.data.util.TypeInformation;

/**
 * This class implements Spring Data's PersistentEntity interface, scavenging the required data from the OGM's mapping
 * classes in order to for SDN to play nicely with Spring Data REST. The main thing to note is that this class is
 * effectively a shim for ClassInfo. We don't reload all the mapping information again.
 * <p>
 * These attributes do not appear to be used/needed for SDN 4 to inter-operate correctly with SD-REST:
 * </p>
 * <ul>
 * <li>typeAlias</li>
 * <li>typeInformation</li>
 * <li>preferredConstructor (we always use the default constructor)</li>
 * <li>versionProperty</li>
 * </ul>
 * Consequently their associated getter methods always return default values of null or [true|false] However, because
 * these method calls are not expected, we also log a warning message if they get invoked
 *
 * @author Vince Bickers
 * @author Adam George
 * @author Mark Paluch
 * @author Michael J. Simons
 * @since 4.0.0
 */
public class Neo4jPersistentEntity<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty> {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jPersistentEntity.class);

	/**
	 * Constructs a new {@link Neo4jPersistentEntity} based on the given type information.
	 *
	 * @param information The {@link TypeInformation} upon which to base this persistent entity.
	 */
	public Neo4jPersistentEntity(TypeInformation<T> information) {
		super(information);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#returnPropertyIfBetterIdPropertyCandidateOrNull(PersistentProperty)
	 */
	@Override
	protected Neo4jPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(Neo4jPersistentProperty property) {

		if (!property.isIdProperty()) {
			return null;
		}

		Neo4jPersistentProperty existingIdProperty = this.getIdProperty();
		Neo4jPersistentProperty preferredIdProperty = existingIdProperty;

		if (existingIdProperty == null) {
			preferredIdProperty = property;
		} else if (existingIdProperty.getPropertyType() == property.getPropertyType()) {
			throw new MappingException(
					String.format("Attempt to add id property %s but already have property %s registered "
									+ "as id. Check your mapping configuration!", property.getField(),
							existingIdProperty.getField()));
		} else if(existingIdProperty.getPropertyType() == PropertyType.INTERNAL_ID_PROPERTY && property.getPropertyType() == PropertyType.ID_PROPERTY) {
			preferredIdProperty = property;
		}
		return preferredIdProperty;
	}

	@Override
	public boolean hasVersionProperty() {
		logger.debug("[entity].hasVersionProperty() returns false"); // by design
		return false;
	}

	@Override
	public Neo4jPersistentProperty getVersionProperty() {
		logger.debug("[entity].getVersionProperty() returns null"); // by design
		return null;
	}

	@Override
	public boolean isVersionProperty(PersistentProperty<?> property) {
		logger.debug("[entity].isIdProperty({}) returns false", property); // again, by design
		return false;
	}

}
