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

import java.util.*;
import java.util.Map.Entry;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.entityaccess.*;
import org.neo4j.ogm.metadata.BaseClassNotFoundException;
import org.neo4j.ogm.metadata.MappingException;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.model.NodeModel;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.model.RelationshipModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class GraphEntityMapper implements GraphToEntityMapper<GraphModel> {

    private final Logger logger = LoggerFactory.getLogger(GraphEntityMapper.class);

    private final MappingContext mappingContext;
    private final EntityFactory entityFactory;
    private final MetaData metadata;
    private final EntityAccessStrategy entityAccessStrategy;

    public GraphEntityMapper(MetaData metaData, MappingContext mappingContext) {
        this.metadata = metaData;
        this.entityFactory = new EntityFactory(metadata);
        this.mappingContext = mappingContext;
        this.entityAccessStrategy = new DefaultEntityAccessStrategy();
    }

    @Override
    public <T> Set<T> map(Class<T> type, GraphModel graphModel) {
        mapEntities(type, graphModel);
        try {
            Set<T> set = new HashSet<>();
            for (Object o : mappingContext.getAll(type)) {
                // can't use the "type" argument to determine ClassInfo because it might be an interface as of DATAGRAPH-577
                ClassInfo classInfo = metadata.classInfo(o.getClass().getName());
                PropertyReader graphIdReader = entityAccessStrategy.getIdentityPropertyReader(classInfo);
                Long id = (Long) graphIdReader.read(o);
                if (classInfo.annotationsInfo().get(RelationshipEntity.CLASS) != null) {
                    if (graphModel.containsRelationshipWithId(id)) {
                        set.add(type.cast(o));
                    }
                }
                else if (graphModel.containsNodeWithId(id)) {
                    set.add(type.cast(o));
                }
            }
            return set;
        } catch (Exception e) {
            throw new MappingException("Error mapping GraphModel to instance of " + type.getName(), e);
        }
    }

    private <T> void mapEntities(Class<T> type, GraphModel graphModel) {
        try {
            mapNodes(graphModel);
            mapRelationships(graphModel);
        } catch (Exception e) {
            throw new MappingException("Error mapping GraphModel to instance of " + type.getName(), e);
        }
    }

    private void mapNodes(GraphModel graphModel) {
        for (NodeModel node : graphModel.getNodes()) {
            Object entity = mappingContext.get(node.getId());
            try {
                if (entity == null) {
                    entity = mappingContext.registerNodeEntity(entityFactory.newObject(node), node.getId());
                }
                setIdentity(entity, node.getId());
                setProperties(node, entity);
                mappingContext.remember(entity);
            } catch (BaseClassNotFoundException e) {
                logger.debug(e.getMessage());
            }
        }
    }

    private void setIdentity(Object instance, Long id) {
        ClassInfo classInfo = metadata.classInfo(instance);
        FieldInfo fieldInfo = classInfo.identityField();
        FieldWriter.write(classInfo.getField(fieldInfo), instance, id);
    }

    private void setProperties(NodeModel nodeModel, Object instance) {
        // cache this.
        ClassInfo classInfo = metadata.classInfo(instance);
        for (Property<?, ?> property : nodeModel.getPropertyList()) {
            writeProperty(classInfo, instance, property);
        }
    }

    private void setProperties(RelationshipModel relationshipModel, Object instance) {
        // cache this.
        ClassInfo classInfo = metadata.classInfo(instance);
        if (relationshipModel.getProperties() != null) {
        for (Entry<String, Object> property : relationshipModel.getProperties().entrySet()) {
            writeProperty(classInfo, instance, Property.with(property.getKey(), property.getValue()));
        }}
    }

    private void writeProperty(ClassInfo classInfo, Object instance, Property<?, ?> property) {

        PropertyWriter writer = entityAccessStrategy.getPropertyWriter(classInfo, property.getKey().toString());

        if (writer == null) {
            logger.debug("Unable to find property: {} on class: {} for writing", property.getKey(), classInfo.name());
        } else {
            Object value = property.getValue();
            // merge iterable / arrays and co-erce to the correct attribute type
            if (writer.type().isArray() || Iterable.class.isAssignableFrom(writer.type())) {
                PropertyReader reader = entityAccessStrategy.getPropertyReader(classInfo, property.getKey().toString());
                if (reader != null) {
                    Object currentValue = reader.read(instance);
                    Class<?> paramType = writer.type();
                    if (paramType.isArray()) {
                        value = EntityAccess.merge(paramType, (Iterable<?>) value, (Object[]) currentValue);
                    } else {
                        value = EntityAccess.merge(paramType, (Iterable<?>) value, (Iterable<?>) currentValue);
                    }
                }
            }
            writer.write(instance, value);
        }
    }

    private boolean tryMappingAsSingleton(Object source, Object parameter, RelationshipModel edge) {

        String edgeLabel = edge.getType();
        ClassInfo sourceInfo = metadata.classInfo(source);

        RelationalWriter writer = entityAccessStrategy.getRelationalWriter(sourceInfo, edgeLabel, parameter);
        if (writer != null && writer.forScalar()) {
            writer.write(source, parameter);
            return true;
        }

        return false;
    }

    private void mapRelationships(GraphModel graphModel) {

        final List<RelationshipModel> oneToMany = new ArrayList<>();

        for (RelationshipModel edge : graphModel.getRelationships()) {

            Object source = mappingContext.get(edge.getStartNode());
            Object target = mappingContext.get(edge.getEndNode());

            if (source != null && target != null) {
                // check whether this edge should in fact be handled as a relationship entity
                // This works because a relationship in the graph that has properties must be represented
                // by a domain entity annotated with @RelationshipEntity, and (if it exists) it will be found by
                // metadata.resolve(...)
                ClassInfo relationshipEntityClassInfo = metadata.resolve(edge.getType());

                if (relationshipEntityClassInfo != null) {
                    logger.debug("Found relationship type: {} to map to RelationshipEntity: {}", edge.getType(), relationshipEntityClassInfo.name());

                    // look to see if this relationship already exists in the mapping context.
                    Object relationshipEntity = mappingContext.getRelationshipEntity(edge.getId());

                    // do we know about it?
                    if (relationshipEntity == null) { // no, create a new relationship entity
                        relationshipEntity = createRelationshipEntity(edge, source, target);
                    }

                    // source.setRelationshipEntity if OUTGOING/UNDIRECTED
                    if (!relationshipDirection(source, edge, relationshipEntity).equals(Relationship.INCOMING)) {
                        // try and find a one-to-one writer
                        ClassInfo sourceInfo = metadata.classInfo(source);
                        RelationalWriter writer = entityAccessStrategy.getRelationalWriter(sourceInfo, edge.getType(), relationshipEntity);

                        if (writer == null) {
                            throw new RuntimeException("no writer for " + source);
                        }

                        if (writer.forScalar()) {
                            writer.write(source, relationshipEntity);
                            mappingContext.registerRelationship(new MappedRelationship(edge.getStartNode(), edge.getType(), edge.getEndNode(), edge.getId()));
                        } else {
                            oneToMany.add(edge);
                        }
                    }
                    // target.setRelationshipEntity if INCOMING/UNDIRECTED
                    if (!relationshipDirection(target, edge, relationshipEntity).equals(Relationship.OUTGOING)) {
                        ClassInfo targetInfo = metadata.classInfo(target);
                        RelationalWriter writer = entityAccessStrategy.getRelationalWriter(targetInfo, edge.getType(), relationshipEntity);

                        if (writer == null) {
                            throw new RuntimeException("no writer for " + target);
                        }

                        if (writer.forScalar()) {
                            writer.write(target, relationshipEntity);
                        } else {
                            oneToMany.add(edge);
                        }
                    }
                }
                else {
                    boolean oneToOne = true;
                    if (!relationshipDirection(source, edge, target).equals(Relationship.INCOMING)) {
                        oneToOne &= tryMappingAsSingleton(source, target, edge);
                    }
                    if (!relationshipDirection(target, edge, source).equals(Relationship.OUTGOING)) {
                        oneToOne &= tryMappingAsSingleton(target, source, edge);

                    }
                    if (!oneToOne) {
                        oneToMany.add(edge);
                    } else {
                        mappingContext.registerRelationship(new MappedRelationship(edge.getStartNode(), edge.getType(), edge.getEndNode(), edge.getId()));
                    }
                }
            } else {
                logger.debug("Relationship {} cannot be hydrated because one or more required node types are not mapped to entity classes", edge);
            }
        }
        mapOneToMany(oneToMany);
    }

    private Object createRelationshipEntity(RelationshipModel edge, Object startEntity, Object endEntity) {

        // create and hydrate the new RE
        Object relationshipEntity = entityFactory.newObject(edge);
        setIdentity(relationshipEntity, edge.getId());
        setProperties(edge, relationshipEntity);
        // REs also have properties
        mappingContext.remember(relationshipEntity);

        // register it in the mapping context
        mappingContext.registerRelationshipEntity(relationshipEntity, edge.getId());

        // set the start and end entities
        ClassInfo relEntityInfo = metadata.classInfo(relationshipEntity);
        RelationalWriter startNodeWriter = entityAccessStrategy.getRelationalEntityWriter(relEntityInfo, StartNode.class);
        if (startNodeWriter != null) {
            startNodeWriter.write(relationshipEntity, startEntity);
        }
        else {
            throw new RuntimeException("Cannot find a writer for the StartNode of relational entity " + relEntityInfo.name());
        }

        RelationalWriter endNodeWriter = entityAccessStrategy.getRelationalEntityWriter(relEntityInfo, EndNode.class);
        if (endNodeWriter != null) {
            endNodeWriter.write(relationshipEntity, endEntity);
        }
        else {
            throw new RuntimeException("Cannot find a writer for the EndNode of relational entity " + relEntityInfo.name());
        }

        return relationshipEntity;
    }

    public Set<Object> get(Class<?> clazz) {
        return mappingContext.getAll(clazz);
    }

    private void mapOneToMany(Collection<RelationshipModel> oneToManyRelationships) {

        EntityCollector typeRelationships = new EntityCollector();

        // first, build the full set of related entities of each type for each source entity in the relationship
        for (RelationshipModel edge : oneToManyRelationships) {

            Object instance = mappingContext.get(edge.getStartNode());
            Object parameter = mappingContext.get(edge.getEndNode());

            // is this a relationship entity we're trying to map?
            Object relationshipEntity = mappingContext.getRelationshipEntity(edge.getId());
            if (relationshipEntity != null) {
                // establish a relationship between
                if (!relationshipDirection(instance, edge, relationshipEntity).equals(Relationship.INCOMING)) {
                    typeRelationships.recordTypeRelationship(instance, relationshipEntity, edge.getType());
                }
                if (!relationshipDirection(parameter, edge, relationshipEntity).equals(Relationship.OUTGOING)) {
                    typeRelationships.recordTypeRelationship(parameter, relationshipEntity,edge.getType());
                }
            }
            else {
                if (!relationshipDirection(instance, edge, parameter).equals(Relationship.INCOMING)) {
                    typeRelationships.recordTypeRelationship(instance, parameter,edge.getType());
                }
                if (!relationshipDirection(parameter, edge, instance).equals(Relationship.OUTGOING)) {
                    typeRelationships.recordTypeRelationship(parameter, instance,edge.getType());
                }
            }
        }

        // then set the entire collection at the same time.
        for (Object instance : typeRelationships.getOwningTypes()) {
            for (String relationshipType : typeRelationships.getOwningRelationshipTypes(instance)) {
                Collection<?> entities = typeRelationships.getCollectiblesForOwnerAndRelationshipType(instance,relationshipType);
                Class entityType = typeRelationships.getCollectibleTypeForOwnerAndRelationshipType(instance, relationshipType);
                mapOneToMany(instance, entityType, entities,relationshipType);
            }
        }

        // finally register all the relationships in the mapping context
        for (RelationshipModel edge : oneToManyRelationships) {
            mappingContext.registerRelationship(new MappedRelationship(edge.getStartNode(), edge.getType(), edge.getEndNode(), edge.getId()));
        }
    }

    /**
     * Returns the relationship direction between source and target of the specified type as specified
     * by the source entity. This may be different from the direction registered in the graph.
     *
     * By default, if a direction is not specified through an annotation, the direction follows
     * the graph's convention, (source)-[:R]->(target) or OUTGOING from source to target.
     *
     * @param source an entity representing the start of the relationship from the graph's perspective
     * @param edge   {@link RelationshipModel} holding information about the relationship in the graph
     * @param target en entity representing the end of the relationship from the graph's perspective
     * @return  one of {@link Relationship.OUTGOING}, {@link Relationship.INCOMING}, {@link Relationship.UNDIRECTED}
     */
    private String relationshipDirection(Object source, RelationshipModel edge, Object target) {
        ClassInfo classInfo = metadata.classInfo(source);
        RelationalWriter writer = entityAccessStrategy.getRelationalWriter(classInfo, edge.getType(), target);
        if (writer == null) {
            writer = entityAccessStrategy.getIterableWriter(classInfo, target.getClass(),edge.getType());
            // will occur if there is no relationship specified on a relationship entity
            if (writer == null) {
                return Relationship.OUTGOING;  // the default
            }
        }
        return writer.relationshipDirection();
    }

    private boolean mapOneToMany(Object instance, Class<?> valueType, Object values, String relationshipType) {

        ClassInfo classInfo = metadata.classInfo(instance);

        // TODO: should just have one kind of relationshipWriter
        RelationalWriter writer = entityAccessStrategy.getIterableWriter(classInfo, valueType, relationshipType);
        if (writer != null) {
            if (writer.type().isArray() || Iterable.class.isAssignableFrom(writer.type())) {
                RelationalReader reader = entityAccessStrategy.getIterableReader(classInfo, valueType, relationshipType);
                Object currentValues;
                if (reader != null) {
                    currentValues = reader.read(instance);
                    if (writer.type().isArray()) {
                        values = EntityAccess.merge(writer.type(), (Iterable<?>) values, (Object[]) currentValues);
                    } else {
                        values = EntityAccess.merge(writer.type(), (Iterable<?>) values, (Iterable<?>) currentValues);
                    }
                }
            }
            writer.write(instance, values);

            return true;
        }

        // this is not necessarily an error. but we can't tell.
        logger.debug("Unable to map iterable of type: {} onto property of {}", valueType, classInfo.name());
        return false;
    }

}
