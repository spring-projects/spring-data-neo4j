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

import org.neo4j.ogm.metadata.MappingException;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.util.TypeInformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements Spring Data's MappingContext interface, scavenging the required data from the
 * OGM's metadata in order to for SDN to play nicely with Spring Data REST.
 *
 * The main thing to note is that this class is effectively a container shim for ClassInfo objects. We don't reload
 * all the mapping information again. 
 *
 * @author: Vince Bickers
 * @since 4.0.0
 *
 */
public class Neo4jMappingContext implements MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty>, ApplicationEventPublisherAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jMappingContext.class);

    private final Map<Class<?>, Neo4jPersistentEntity> persistentEntities = new HashMap<>();

    private ApplicationEventPublisher applicationEventPublisher;

    public Neo4jMappingContext(MetaData metaData) {
        logger.debug("[context].Neo4jMappingContext initialised with OGM Metadata: " + metaData.persistentEntities().size() + " classes");

        for (ClassInfo classInfo : metaData.persistentEntities()) {
            // todo: exclude top level Object class from metadata
            if (classInfo.name().equals("java.lang.Object")) {
                continue;
            }

            try {
                FieldInfo identityField = classInfo.identityField();
                try {
                    Class<?> clazz = Class.forName(classInfo.name());
                    Neo4jPersistentEntity neo4jPersistentEntity = new Neo4jPersistentEntity(clazz, classInfo, identityField);
                    logger.debug("[context].Neo4jMappingContext added persistentEntity for: " + classInfo.name());
                    persistentEntities.put(clazz, neo4jPersistentEntity);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (MappingException me) {
                logger.debug(me.getMessage());
            }
        }

    }

    @Override
    public Collection getPersistentEntities() {
        logger.debug("[context].getPersistentEntities() called");
        return persistentEntities.values();
    }

    @Override
    public Neo4jPersistentEntity<?> getPersistentEntity(Class<?> type) {
        logger.debug("[context].getPersistentEntity() called for type {}", type);
        return persistentEntities.get(type);
    }

    @Override
    public boolean hasPersistentEntityFor(Class<?> type) {
        logger.debug("[context].hasPersistentEntity() called for type {}", type);
        return persistentEntities.containsKey(type);
    }

    @Override
    public Neo4jPersistentEntity getPersistentEntity(TypeInformation<?> typeInfo) {
        logger.warn("[context].getPersistentEntity() called but not implemented");
        //return persistentEntities.get(typeInfo.getType());
        return null;
    }

    @Override
    public Neo4jPersistentEntity getPersistentEntity(Neo4jPersistentProperty persistentProperty) {
        logger.warn("[context].getPersistentEntity({}) called but not implemented", persistentProperty.getName());
        return null;
    }

    @Override
    public PersistentPropertyPath getPersistentPropertyPath(PropertyPath propertyPath) {
        logger.warn("[context].getPersistentPropertyPath({}) called but not implemented", propertyPath);
        return null;
    }

    @Override
    public PersistentPropertyPath getPersistentPropertyPath(String propertyPath, Class<?> type) {
        logger.warn("[context].getPersistentPropertyPath({}, {}) called but not implemented", propertyPath, type);
        return null;
    }

    @Override
    public Collection<TypeInformation<?>> getManagedTypes() {
        logger.warn("[context].getManagedTypes() called but not implemented");
        return null;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        logger.debug("[context].setApplicationEventPublisher() called");
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.debug("[context].afterPropertiesSet() called");
    }
}
