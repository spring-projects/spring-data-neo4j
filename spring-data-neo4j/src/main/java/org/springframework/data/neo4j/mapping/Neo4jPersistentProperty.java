/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.Version;
import org.neo4j.ogm.exception.core.MetadataException;
import org.neo4j.ogm.metadata.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * This class implements Spring Data's PersistentProperty interface, scavenging the required data from the OGM's mapping
 * classes in order to for SDN to play nicely with Spring Data REST. The main thing to note is that this class is
 * effectively a shim for FieldInfo. We don't reload all the mapping information again. We do not yet support
 * getter/setter access to entity properties.
 * <p>
 * These attributes do not appear to be used/needed for SDN 4 to inter-operate correctly with SD-REST:
 * </p>
 * <ul>
 * <li>mapValueType</li>
 * <li>typeInformation</li>
 * <li>isVersionProperty (there is no SDN versioning at this stage)</li>
 * <li>isTransient (we never supply transient classes to the Spring mapping context)</li>
 * <li>isWritable (we don't currently support read-only fields)</li>
 * </ul>
 * Consequently their associated getter methods always return default values of null or [true|false] However, because
 * these method calls are not expected, we also log a warning message if they get invoked
 *
 * @author Vince Bickers
 * @author Adam George
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Mark Paluch
 * @since 4.0.0
 */
public class Neo4jPersistentProperty extends AnnotationBasedPersistentProperty<Neo4jPersistentProperty> {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jPersistentProperty.class);

	private final boolean isIdProperty;

	/**
	 * Constructs a new {@link Neo4jPersistentProperty} based on the given arguments.
	 *
	 * @param owningClassInfo The {@link ClassInfo} of the object of which the property field is a member
	 * @param property The property
	 * @param owner The owning {@link PersistentEntity} that corresponds to the given {@code ClassInfo}
	 * @param simpleTypeHolder The {@link SimpleTypeHolder} that dictates whether the type of this property is considered
	 *          simple or not
	 */
	public Neo4jPersistentProperty(ClassInfo owningClassInfo, Property property,
			PersistentEntity<?, Neo4jPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);
		if (owningClassInfo == null) {
			logger.warn("Owning ClassInfo is null for property: {}", property);
		}
		if ((owningClassInfo != null && owningClassInfo.getUnderlyingClass() != null
				&& simpleTypeHolder.isSimpleType(owningClassInfo.getUnderlyingClass())) || owner.getType().isEnum()) { // TODO
																																																								// refactor
																																																								// all
																																																								// these
																																																								// null
																																																								// checks
			this.isIdProperty = false;
		} else {
			this.isIdProperty = resolveWhetherIdProperty(owningClassInfo, property);
		}
	}

	private static boolean resolveWhetherIdProperty(ClassInfo owningClassInfo, Property property) {
		if (owningClassInfo == null || owningClassInfo.isInterface()
				|| owningClassInfo.annotationsInfo().get(QueryResult.class.getName()) != null || owningClassInfo.isEnum()) {
			// no ID properties on @QueryResult or non-concrete objects
			return false;
		} else {
			try {
				return property.getField() //
						.filter(field -> owningClassInfo.getField(owningClassInfo.identityField()).equals(field)) //
						.isPresent();
			} catch (MetadataException noIdentityField) {
				logger.warn("No identity field found for class of type: {} when creating persistent property for : {}",
						owningClassInfo.name(), property);
				return false;
			}
		}
	}

	@Override
	public boolean isIdProperty() {
		logger.debug("[property].isIdProperty() returns {}", this.isIdProperty);
		return this.isIdProperty;
	}

	@Override
	public boolean isVersionProperty() {
		return isAnnotationPresent(Version.class);
	}

	/**
	 * Overridden to force field access as opposed to getter method access for simplicity.
	 *
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#usePropertyAccess()
	 */
	@Override
	public boolean usePropertyAccess() {
		logger.debug("[property].usePropertyAccess() returns false");
		return false;
	}

	/**
	 * Determines whether or not this property should be considered an association to another entity or whether it's just
	 * a simple property that should be shown as a value.
	 * <p>
	 * This implementation works by looking for non-transient members annotated with <code>@Relationship</code>.
	 * </p>
	 *
	 * @return <code>true</code> if this property is an association to another entity, <code>false</code> if not
	 */
	@Override
	public boolean isAssociation() {
		return !isTransient() && (isAnnotationPresent(Relationship.class) || isAnnotationPresent(StartNode.class)
				|| isAnnotationPresent(EndNode.class));
	}

	@Override
	protected Association<Neo4jPersistentProperty> createAssociation() {
		return new Association<Neo4jPersistentProperty>(this, null);
	}
}
