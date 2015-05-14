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

import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.*;
import org.springframework.data.util.TypeInformation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class implements Spring Data's PersistentEntity interface, scavenging the required data from the
 * OGM's mapping classes in order to for SDN to play nicely with Spring Data REST.
 *
 * The main thing to note is that this class is effectively a shim for ClassInfo. We don't reload
 * all the mapping information again.
 *
 * These attributes do not appear to be used/needed for SDN 4 to inter-operate correctly with SD-REST:
 *
 *      typeAlias
 *      typeInformation
 *      preferredConstructor (we always use the default constructor)
 *      versionProperty
 *
 * Consequently their associated getter methods always return default values of null or [true|false]
 * However, because these method calls are not expected, we also log a warning message if they get invoked
 *
 * @author: Vince Bickers
 * @since 4.0.0
 *
 */
public class Neo4jPersistentEntity<T> implements PersistentEntity<T, Neo4jPersistentProperty> {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jPersistentEntity.class);

    private String name;
    private Neo4jPersistentProperty idProperty;
    private Map<String, Neo4jPersistentProperty> properties = new HashMap<>();
    private Set<Association> associations = new HashSet<>();
    private Class<T> type;

    public Neo4jPersistentEntity(Class<T> clazz, ClassInfo classInfo, FieldInfo identityField) {

        this.name = classInfo.name();
        this.type = clazz;

        for (FieldInfo fieldInfo : classInfo.fieldsInfo().fields()) {
            String name = fieldInfo.getName();

            logger.debug("[entity].creating property mapping {}.{}", this.name, fieldInfo.getName());

            Neo4jPersistentProperty property = new Neo4jPersistentProperty(this, classInfo, fieldInfo, (fieldInfo == identityField));
            this.properties.put(name, property);


            if (fieldInfo == identityField) {
                logger.debug("[entity].this is the identity property");
                this.idProperty = property;
            }
            else if (property.isAssociation()) {
                logger.debug("[entity].this is an association property");
                associations.add(new Association(property, null));
            } else {
                logger.debug("[entity].this is a simple property");
            }
        }

    }

    @Override
    public String getName() {
        logger.debug("[entity].getName() returns");
        return this.name;
    }

    @Override
    public PreferredConstructor<T, Neo4jPersistentProperty> getPersistenceConstructor() {
        logger.warn("[entity].getPersistenceConstructor() called but not implemented");
        return null;
    }

    @Override
    public boolean isConstructorArgument(PersistentProperty<?> property) {
        logger.debug("[entity].isConstructorArgument({}) returns false", property);
        return false;
    }

    @Override
    public boolean isIdProperty(PersistentProperty<?> property) {
        logger.debug("[entity].isIdProperty() returns {}", (this.idProperty == property));
        return this.idProperty == property;
    }

    @Override
    public boolean isVersionProperty(PersistentProperty<?> property) {
        logger.debug("[entity].isIdProperty({}) returns false", property);
        return false;
    }

    @Override
    public Neo4jPersistentProperty getIdProperty() {
        logger.debug("[entity].getIdProperty() returns {}", this.idProperty);
        return this.idProperty;
    }

    @Override
    public Neo4jPersistentProperty getVersionProperty() {
        logger.debug("[entity].getVersionProperty() returns null"); // by design
        return null;
    }

    @Override
    public Neo4jPersistentProperty getPersistentProperty(String name) {
        logger.debug("[entity].getPersistentProperty({}) returns {}", name, this.properties.get(name));
        return this.properties.get(name);
    }

    @Override
    public Neo4jPersistentProperty getPersistentProperty(Class<? extends Annotation> annotationType) {
        logger.warn("[entity].getPersistentProperty({}) called but not implemented", annotationType);
        return null;
    }

    @Override
    public boolean hasIdProperty() {
        logger.debug("[entity].hasIdProperty() returns {}", (this.idProperty != null));
        return this.idProperty != null;
    }

    @Override
    public boolean hasVersionProperty() {
        logger.debug("[entity].hasVersionProperty() returns false");  // by design
        return false;
    }

    @Override
    public Class<T> getType() {
        logger.debug("[entity].getType() returns {}", this.type);
        return this.type;
    }

    @Override
    public Object getTypeAlias() {
        logger.debug("[entity].getTypeAlias() called but not implemented");
        return null;
    }

    @Override
    public TypeInformation<T> getTypeInformation() {
        logger.warn("[entity].getTypeInformation() called but not implemented");
        return null;
    }

    @Override
    public void doWithProperties(PropertyHandler<Neo4jPersistentProperty> handler) {
        logger.warn("[entity].doWithProperties({}) called but not implemented", handler);
    }

    @Override
    public void doWithProperties(SimplePropertyHandler handler) {
        logger.warn("[entity].doWithProperties({}) called but not implemented", handler);
    }

    @Override
    public void doWithAssociations(AssociationHandler<Neo4jPersistentProperty> handler) {
        logger.warn("[entity].doWithAssociations({}) called but not implemented", handler);
    }

    @Override
    public void doWithAssociations(SimpleAssociationHandler handler) {
        logger.debug("[entity].doWithAssociations({handler}) called");
        for (Association<?> association : associations) {
            handler.doWithAssociation(association);
        }
    }

    @Override
    public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
        logger.warn("[entity].findAnnotation({}) called but not implemented", annotationType);
        return null;
    }

    @Override
    public PersistentPropertyAccessor getPropertyAccessor(Object bean) {
        logger.warn("[entity].getPropertyAccessor({}) called but not implemented", bean);
        return null;
    }

    @Override
    public IdentifierAccessor getIdentifierAccessor(Object bean) {
        logger.warn("[entity].getIdentifierAccessor({}) called but not implemented", bean);
        return null;
    }
}
