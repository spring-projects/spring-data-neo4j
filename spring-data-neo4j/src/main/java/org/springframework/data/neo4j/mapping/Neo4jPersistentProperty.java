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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.neo4j.ogm.metadata.info.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AbstractPersistentProperty;
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
public class Neo4jPersistentProperty extends AbstractPersistentProperty<Neo4jPersistentProperty> {

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
            this.isIdProperty = owningClassInfo.getField(owningClassInfo.identityField()).equals(field);
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

    @Override
    public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
        logger.debug("[property].getAnnotation({}) returns {}", annotationType, field.getAnnotation(annotationType));
        return field.getAnnotation(annotationType);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        logger.debug("[property].isAnnotationPresent({}) returns {}", annotationType, field.isAnnotationPresent(annotationType));
        return field.isAnnotationPresent(annotationType);
    }

    @Override
    // force to use field access
    public boolean usePropertyAccess() {
        logger.debug("[property].usePropertyAccess() returns false");  // by design
        return false;
    }

    @Override
    protected Association<Neo4jPersistentProperty> createAssociation() {
        logger.warn("[property].createAssociation({}) called but not implemented");
        return null;
    }

    @Override
    public <A extends Annotation> A findPropertyOrOwnerAnnotation(Class<A> annotationType) {
        logger.debug("[property].findPropertyOrOwnerAnnotation({}) called");
        A annotation = findAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        return owner.findAnnotation(annotationType);
    }

}
