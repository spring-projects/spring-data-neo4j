/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */
package org.springframework.data.neo4j.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.metadata.MappingException;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * This class implements Spring Data's PersistentProperty interface, scavenging the required data from the
 * OGM's mapping classes in order to for SDN to play nicely with Spring Data REST.
 *
 * The main thing to note is that this class is effectively a shim for FieldInfo. We don't reload
 * all the mapping information again.
 *
 * We do not yet support getter/setter access to entity properties.
 * <p>
 * These attributes do not appear to be used/needed for SDN 4 to inter-operate correctly with SD-REST:
 * </p>
 * <ul>
 *   <li>mapValueType</li>
 *   <li>typeInformation</li>
 *   <li>isVersionProperty (there is no SDN versioning at this stage)</li>
 *   <li>isTransient (we never supply transient classes to the Spring mapping context)</li>
 *   <li>isWritable (we don't currently support read-only fields)</li>
 * </ul>
 * Consequently their associated getter methods always return default values of null or [true|false]
 * However, because these method calls are not expected, we also log a warning message if they get invoked
 *
 * @author Vince Bickers
 * @author Adam George
 * @since 4.0.0
 */
public class Neo4jPersistentProperty extends AnnotationBasedPersistentProperty<Neo4jPersistentProperty> {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jPersistentProperty.class);

    private final boolean isIdProperty;

    /**
     * Constructs a new {@link Neo4jPersistentProperty} based on the given arguments.
     *
     * @param owningClassInfo The {@link ClassInfo} of the object of which the property field is a member
     * @param field The property {@link Field}
     * @param descriptor The Java bean {@link PropertyDescriptor}
     * @param owner The owning {@link PersistentEntity} that corresponds to the given {@code ClassInfo}
     * @param simpleTypeHolder The {@link SimpleTypeHolder} that dictates whether the type of this property is considered simple
     *        or not
     */
    public Neo4jPersistentProperty(ClassInfo owningClassInfo, Field field, PropertyDescriptor descriptor,
            PersistentEntity<?, Neo4jPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
        super(field, descriptor, owner, simpleTypeHolder);

        if (owningClassInfo.isInterface() || owningClassInfo.annotationsInfo().get(QueryResult.class.getName()) != null) {
            // no ID properties on @QueryResult or non-concrete objects
            this.isIdProperty = false;
        } else {
            boolean idProperty = false;
            try {
                // crash prevention - hopefully won't be here too long
                idProperty = owningClassInfo.getField(owningClassInfo.identityField()).equals(field);
            } catch (MappingException me) {
                logger.error("Error finding identity field on " + owningClassInfo.name(), me);
            }
            this.isIdProperty = idProperty;
        }
    }

    @Override
    public boolean isIdProperty() {
        logger.debug("[property].isIdProperty() returns {}", this.isIdProperty);
        return this.isIdProperty;
    }

    @Override
    public boolean isVersionProperty() {
        logger.debug("[property].isVersionProperty() returns false"); // by design
        return false;
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
     * Determines whether or not this property should be considered an association to another entity or whether it's
     * just a simple property that should be shown as a value.
     * <p>
     * This implementation works by looking for non-transient members annotated with <code>@Relationship</code>.
     * </p>
     *
     * @return <code>true</code> if this property is an association to another entity, <code>false</code> if not
     */
    @Override
    public boolean isAssociation() {
        // TODO: can we also work out whether the target class is a node/relationship entity?
        return !isTransient() && isAnnotationPresent(Relationship.class);
    }

    @Override
    protected Association<Neo4jPersistentProperty> createAssociation() {
        return new Association<Neo4jPersistentProperty>(this, null);
    }

}
