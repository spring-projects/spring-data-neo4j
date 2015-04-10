/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.mapper;

import org.neo4j.ogm.entityaccess.DefaultEntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityAccessStrategy;
import org.neo4j.ogm.entityaccess.PropertyReader;
import org.neo4j.ogm.entityaccess.RelationalReader;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The MappingContext maintains a map of all the objects created during the hydration
 * of an object map (domain hierarchy). The MappingContext lifetime is concurrent
 * with a session lifetime.
 *
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class MappingContext {

    // we need multiple registers whose purpose is obvious from their names:

    // NodeEntityRegister               register of domain entities whose properties are to be stored on Nodes
    // RelationshipEntityRegister       register of domain entities whose properties are to be stored on Relationships
    // RelationshipRegister             register of relationships between NodeEntities (i.e. as they are in the graph)

    private final Logger logger = LoggerFactory.getLogger(MappingContext.class);

    private final ConcurrentMap<Long, Object> relationshipEntityRegister = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Object> nodeEntityRegister = new ConcurrentHashMap<>();
    private final Set<MappedRelationship> relationshipRegister = new HashSet<>();

    /** register of all mapped entities of a specific type (including supertypes) */
    private final ConcurrentMap<Class<?>, Set<Object>> typeRegister = new ConcurrentHashMap<>();
    private final EntityMemo objectMemo = new EntityMemo();

    private final MetaData metaData;
    private final EntityAccessStrategy entityAccessStrategy = new DefaultEntityAccessStrategy();

    public MappingContext(MetaData metaData) {
        this.metaData = metaData;
    }

    public Object get(Long id) {
        return nodeEntityRegister.get(id);
    }

    public Object registerNodeEntity(Object entity, Long id) {
        nodeEntityRegister.putIfAbsent(id, entity);
        entity = nodeEntityRegister.get(id);
        registerTypes(entity.getClass(), entity);
        return entity;
    }

    private void registerTypes(Class type, Object entity) {
        getAll(type).add(entity);
        if (type.getSuperclass() != null
                && metaData != null
                && metaData.classInfo(type.getSuperclass().getName()) != null
                && !type.getSuperclass().getName().equals("java.lang.Object")) {
            registerTypes(type.getSuperclass(), entity);
        }
        if(type.getInterfaces() != null
                && metaData!=null) {
            for(Class interfaceClass : type.getInterfaces()) {
                if(metaData.classInfo(interfaceClass.getName())!=null) {
                    registerTypes(interfaceClass,entity);
                }
            }
        }
    }

    private void deregisterTypes(Class type, Object entity) {
        Set<Object> entities = typeRegister.get(type);
        if (entities != null) {
            if (type.getSuperclass() != null
                    && metaData != null
                    && metaData.classInfo(type.getSuperclass().getName()) != null
                    && !type.getSuperclass().getName().equals("java.lang.Object")) {
                deregisterTypes(type.getSuperclass(), entity);
            }
        }
    }

    /**
     * Deregisters an object from the mapping context
     * - removes the object instance from the typeRegister(s)
     * - removes the object id from the nodeEntityRegister
     *
     * @param entity the object to deregister
     * @param id the id of the object in Neo4j
     */
    public void deregister(Object entity, Long id) {
        deregisterTypes(entity.getClass(), entity);
        nodeEntityRegister.remove(id);
    }

    public void replace(Object entity, Long id) {
        nodeEntityRegister.remove(id);
        registerNodeEntity(entity, id);
        remember(entity);
    }

    public Set<Object> getAll(Class<?> type) {
        Set<Object> objectList = typeRegister.get(type);
        if (objectList == null) {
            typeRegister.putIfAbsent(type, Collections.synchronizedSet(new HashSet<>()));
            objectList = typeRegister.get(type);
        }
        return objectList;
    }

    // object memorisations
    public void remember(Object entity) {
        objectMemo.remember(entity, metaData.classInfo(entity));
    }

    public boolean isDirty(Object entity) {
        ClassInfo classInfo = metaData.classInfo(entity);
        return !objectMemo.remembered(entity, classInfo);
    }

    // these methods belong on the relationship registry
    public boolean isRegisteredRelationship(MappedRelationship relationship) {
        return relationshipRegister.contains(relationship);
    }

    public Set<MappedRelationship> mappedRelationships() {
        return relationshipRegister;
    }

    public void registerRelationship(MappedRelationship relationship) {
        relationshipRegister.add(relationship);
    }

    public void clear() {
        objectMemo.clear();
        relationshipRegister.clear();
        nodeEntityRegister.clear();
        typeRegister.clear();
        relationshipEntityRegister.clear();
    }


    // relationshipentity methods
    public Object getRelationshipEntity(Long relationshipId) {
        return relationshipEntityRegister.get(relationshipId);
    }

    public Object registerRelationshipEntity(Object relationshipEntity, Long id) {
        relationshipEntityRegister.putIfAbsent(id, relationshipEntity);
        registerTypes(relationshipEntity.getClass(), relationshipEntity);
        return relationshipEntity;
    }

    /**
     * purges all information about objects of the supplied type
     * from the mapping context
     *
     * @param type the type whose object references and relationship mappings we want to purge
     */
    public void clear(Class<?> type) {

        ClassInfo classInfo = metaData.classInfo(type.getName());
        PropertyReader identityReader = entityAccessStrategy.getIdentityPropertyReader(classInfo);
        for (Object entity : getAll(type)) {
            purge(entity, identityReader);
        }
        getAll(type).clear();
    }

    /**
     * purges all information about this object from the mapping context
     *
     * @param entity the type whose object references and relationship mappings we want to purge
     */
    public void clear(Object entity) {
        Class<?> type = entity.getClass();
        ClassInfo classInfo = metaData.classInfo(type.getName());
        PropertyReader identityReader = entityAccessStrategy.getIdentityPropertyReader(classInfo);
        purge(entity, identityReader);
        getAll(type).remove(entity);
    }

    private void purge(Object entity, PropertyReader identityReader) {
        Long id = (Long) identityReader.read(entity);
        if (id != null) {
            if (nodeEntityRegister.containsValue(entity)) {
                nodeEntityRegister.remove(id);

                // remove all relationship mappings to/from this object
                Iterator<MappedRelationship> mappedRelationshipIterator = mappedRelationships().iterator();
                while (mappedRelationshipIterator.hasNext()) {
                    MappedRelationship mappedRelationship = mappedRelationshipIterator.next();
                    if (mappedRelationship.getStartNodeId() == id || mappedRelationship.getEndNodeId() == id) {
                        mappedRelationshipIterator.remove();
                    }
                }
            }
            if (relationshipEntityRegister.containsValue(entity)) {
                relationshipEntityRegister.remove(id);
                RelationalReader startNodeReader = entityAccessStrategy.getStartNodeReader(metaData.classInfo(entity));
                clear(startNodeReader.read(entity));
                RelationalReader endNodeReader = entityAccessStrategy.getEndNodeReader(metaData.classInfo(entity));
                clear(endNodeReader.read(entity));
            }
        }
    }

    public void dump() {

        for (Object o : nodeEntityRegister.values()) {
            boolean remembered = objectMemo.contains(o);
            System.out.println(String.format("%s, %s", o, remembered));
        }

        for (Object o : relationshipEntityRegister.values()) {
            boolean remembered = objectMemo.contains(o);
            System.out.println(String.format("%s, %s", o, remembered));
        }
    }
}
