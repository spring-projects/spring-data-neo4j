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
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * This class implements Spring Data's PersistentProperty interface, scavenging the required data from the
 * OGM's mapping classes in order to for SDN to play nicely with Spring Data REST.
 *
 * The main thing to note is that this class is effectively a shim for FieldInfo. We don't reload
 * all the mapping information again. 
 * 
 * We do not yet support getter/setter access to entity properties.
 *
 * These attributes do not appear to be used/needed for SDN 4 to inter-operate correctly with SD-REST:
 *
 *      mapValueType
 *      typeInformation
 *      isEntity
 *      isMap
 *      isTransient (we never supply transient classes to the Spring mapping context)
 *      isWritable (we don't currently support read-only fields)
 *
 * Consequently their associated getter methods always return default values of null or [true|false]
 * However, because these method calls are not expected, we also log a warning message if they get invoked
 *
 * @author: Vince Bickers
 * @since 4.0.0
 *
 */
public class Neo4jPersistentProperty implements PersistentProperty<Neo4jPersistentProperty> {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jPersistentProperty.class);

    private Field field;
    private Class<?> type;
    private String name;
    private boolean isCollectionLike;
    private boolean isArray;
    private Neo4jPersistentEntity owner;
    private boolean isAssociation;
    private Class<?> rawType;
    private Class<?> actualType;
    private Class<?> componentType;
    private boolean isIdProperty;

    public Neo4jPersistentProperty(Neo4jPersistentEntity owner, ClassInfo classInfo, FieldInfo fieldInfo, boolean idProperty) {

        try {
            this.field = classInfo.getField(fieldInfo);

            this.owner = owner;
            this.name = fieldInfo.getName();

            this.type = field.getType();
            this.rawType = this.type;
            this.actualType = this.type;   // simple type

            this.isAssociation = !fieldInfo.isSimple();
            this.isCollectionLike = !fieldInfo.isScalar();
            this.isArray = fieldInfo.isArray();
            this.isIdProperty = idProperty;

            if (fieldInfo.isCollection()) {
                this.actualType = classInfo.getType(fieldInfo.getTypeParameterDescriptor());
                this.componentType = actualType;
            } else if (fieldInfo.isArray()) {
                this.actualType = field.getType().getComponentType();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PersistentEntity<?, Neo4jPersistentProperty> getOwner() {
        logger.debug("[property].getOwner() returns {}", this.owner);
        return this.owner;
    }

    @Override
    public String getName() {
        logger.debug("[property].getName() returns {}", this.name);
        return this.name;
    }

    @Override
    public Class<?> getType() {
        logger.debug("[property].getType() returns {}", this.type);
        return this.type;
    }

    @Override
    public TypeInformation<?> getTypeInformation() {
        logger.warn("[property].getTypeInformation() called but not implemented");
        return null;
    }

    @Override
    public Iterable<? extends TypeInformation<?>> getPersistentEntityType() {
        logger.warn("[property].getPersistentEntityType() called but not implemented");
        return null;
    }

    @Override
    public Method getGetter() {
        logger.debug("[property].getGetter() returns null");   // by design
        return null;
    }

    @Override
    public Method getSetter() {
        logger.debug("[property].getSetter() returns null");  // by design
        return null;
    }

    @Override
    public Field getField() {
        logger.debug("[property].getField() returns {}",this.field);
        return this.field;
    }

    @Override
    public String getSpelExpression() {
        logger.debug("[property].getSpelExpression() returns null"); // by design
        return null;
    }

    @Override
    public Association<Neo4jPersistentProperty> getAssociation() {
        logger.warn("[property].getAssociation() called but not implemented"); // what is this for?
        return null;
    }

    @Override
    public boolean isEntity() {
        logger.warn("[property].isEntity() called but not implemented");
        return false;
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
    public boolean isCollectionLike() {
        logger.debug("[property].isCollectionLike() returns {}", this.isCollectionLike);
        return this.isCollectionLike;
    }

    @Override
    public boolean isMap() {
        logger.warn("[property].isMap() called but not implemented");
        return false;
    }

    @Override
    public boolean isArray() {
        logger.debug("[property].isArray() returns {}", this.isArray);
        return this.isArray;
    }

    @Override
    public boolean isTransient() {
        logger.debug("[property].isTransient() returns false");   // by design
        return false;
    }

    @Override
    public boolean isWritable() {
        logger.debug("[property].isWritable() returns true");   // by design
        return true;
    }

    @Override
    public boolean isAssociation() {
        logger.debug("[property].isAssociation() returns {}", this.isAssociation);
        return this.isAssociation;
    }

    /**
     * Returns the component type of the type if it is a {@link java.util.Collection}. Will return the type of the key if
     * the property is a {@link java.util.Map}.
     *
     * @return the component type, the map's key type or {@literal null} if neither {@link java.util.Collection} nor
     *         {@link java.util.Map}.
     */
    @Override
    public Class<?> getComponentType() {
        logger.debug("[property].getComponentType() returns {}", this.componentType);
        return this.componentType;
    }

    /**
     * Returns the raw type as it's pulled from from the reflected property.
     *
     * @return the raw type of the property.
     */
    @Override
    public Class<?> getRawType() {
        logger.debug("[property].getRawType() returns {}", this.rawType);
        return this.rawType;
    }

    /**
     * Returns the type of the values if the property is a {@link java.util.Map}.
     *
     * @return the map's value type or {@literal null} if no {@link java.util.Map}
     */
    @Override
    public Class<?> getMapValueType() {
        logger.warn("[property].getMapValueType() called but not implemented");
        return null;
    }

    /**
     * Returns the actual type of the property. This will be the original property type if no generics were used, the
     * component type for collection-like types and arrays as well as the value type for map properties.
     *
     * @return
     */
    @Override
    public Class<?> getActualType() {
        logger.debug("[property].getActualType() returns {}", this.actualType);
        return this.actualType;
    }

    @Override
    public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
        logger.debug("[property].getAnnotation({}) returns {}", annotationType, field.getAnnotation(annotationType));
        return field.getAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A findPropertyOrOwnerAnnotation(Class<A> annotationType) {
        logger.warn("[property].findPropertyOrOwnerAnnotation({}) called but not implemented", annotationType);
        /**
        A annotation = findAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        return (A) owner.findAnnotation(annotationType);
        **/
        return null;
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
}
