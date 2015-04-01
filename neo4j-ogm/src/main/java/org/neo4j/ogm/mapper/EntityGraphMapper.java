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

import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.cypher.compiler.*;
import org.neo4j.ogm.entityaccess.DefaultEntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityAccessStrategy;
import org.neo4j.ogm.entityaccess.PropertyReader;
import org.neo4j.ogm.entityaccess.RelationalReader;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.AnnotationInfo;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Implementation of {@link EntityToGraphMapper} that is driven by an instance of {@link MetaData}.
 *
 * @author Vince Bickers
 */
public class EntityGraphMapper implements EntityToGraphMapper {

    private final Logger logger = LoggerFactory.getLogger(EntityGraphMapper.class);

    private final MetaData metaData;
    private final EntityAccessStrategy entityAccessStrategy;
    private final MappingContext mappingContext;

    /**
     * Constructs a new {@link EntityGraphMapper} that uses the given {@link MetaData}.
     *
     * @param metaData The {@link MetaData} containing the mapping information
     * @param mappingContext The {@link MappingContext} for the current session
     */
    public EntityGraphMapper(MetaData metaData, MappingContext mappingContext) {
        this.metaData = metaData;
        this.mappingContext = mappingContext;
        this.entityAccessStrategy = new DefaultEntityAccessStrategy();
    }

    @Override
    public CypherContext map(Object entity) {
        return map(entity, -1);
    }

    @Override
    public CypherContext map(Object entity, int horizon) {

        if (entity == null) {
            throw new NullPointerException("Cannot map null object");
        }

        CypherCompiler compiler = new SingleStatementCypherCompiler();

        // add all the relationships we know about. This includes the relationships that
        // won't be modified by the mapping request.
        for (MappedRelationship mappedRelationship : mappingContext.mappedRelationships()) {
            logger.debug("context-init: (${})-[:{}]->(${})", mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
            compiler.context().registerRelationship(mappedRelationship);
        }

        logger.debug("context initialised with {} relationships", mappingContext.mappedRelationships().size());

        // if the map request is rooted on a relationship entity, we re-root it on the start node
        if (isRelationshipEntity(entity)) {
            entity = entityAccessStrategy.getStartNodeReader(metaData.classInfo(entity)).read(entity);
            if (entity == null) {
                throw new RuntimeException("@StartNode of relationship entity may not be null");
            }
        }

        mapEntity(entity, horizon, compiler);
        deleteObsoleteRelationships(compiler);

        return compiler.compile();
    }


    /**
     * Detects object references (including from lists) that have been deleted in the domain.
     * These must be persisted as explicit requests to delete the corresponding relationship in the graph
     *
     * @param compiler the {@link CypherCompiler} instance.
     */
    private void deleteObsoleteRelationships(CypherCompiler compiler) {
        CypherContext context=compiler.context();
        Iterator<MappedRelationship> mappedRelationshipIterator = mappingContext.mappedRelationships().iterator();

        while (mappedRelationshipIterator.hasNext()) {
            MappedRelationship mappedRelationship = mappedRelationshipIterator.next();
            if (!context.isRegisteredRelationship(mappedRelationship)) {
                logger.debug("context-del: (${})-[:{}]->(${})", mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
                compiler.unrelate("$" + mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), "$" + mappedRelationship.getEndNodeId());
                clearRelatedObjects(mappedRelationship.getStartNodeId());
                mappedRelationshipIterator.remove();
            }
        }
    }

    private void clearRelatedObjects(Long node) {
        for (MappedRelationship mappedRelationship : mappingContext.mappedRelationships()) {
            if (mappedRelationship.getStartNodeId() == node || mappedRelationship.getEndNodeId() == node) {
                Object dirty = mappingContext.get(mappedRelationship.getEndNodeId());
                // forward
                if (dirty != null) {
                    logger.debug("flushing end node of: (${})-[:{}]->(${})", mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
                    mappingContext.deregister(dirty, mappedRelationship.getEndNodeId());
                }
                // reverse
                dirty = mappingContext.get(mappedRelationship.getStartNodeId());
                if (dirty != null) {
                    logger.debug("flushing start node of: (${})-[:{}]->(${})", mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
                    mappingContext.deregister(dirty,  mappedRelationship.getStartNodeId());
                }
            }
        }
    }

    /**
     * Builds Cypher to save the specified object and all its composite objects into the graph database.
     *
     * @param compiler The {@link CypherCompiler} used to construct the query
     * @param entity The object to persist into the graph database as a node
     * @return The "root" node of the object graph that matches
     */
    private NodeBuilder mapEntity(Object entity, int horizon, CypherCompiler compiler) {

        CypherContext context=compiler.context();

        if (context.visited(entity)) {
            logger.debug("already visited: {}", entity);
            return context.nodeBuilder(entity);
        }

        NodeBuilder nodeBuilder = getNodeBuilder(compiler, entity);
        if (nodeBuilder != null) {
            updateNode(entity, context, nodeBuilder);
            if (horizon != 0) {
                mapEntityReferences(entity, nodeBuilder, horizon - 1, compiler);
            } else {
                logger.debug("at horizon: {} ", entity);
            }
        }
        return nodeBuilder;
    }

    /**
     * Creates a new node or updates an existing one in the graph, if it has changed.
     *
     * @param entity the domain object to be persisted
     * @param context  the current {@link CypherContext}
     * @param nodeBuilder a {@link NodeBuilder} that knows how to compile node create/update cypher phrases
     */
    private void updateNode(Object entity, CypherContext context, NodeBuilder nodeBuilder) {
        if (mappingContext.isDirty(entity)) {
            logger.debug("{} has changed", entity);
            context.log(entity);
            ClassInfo classInfo = metaData.classInfo(entity);
            nodeBuilder.mapProperties(entity, classInfo, entityAccessStrategy);
        } else {
            logger.debug("{}, has not changed", entity);
        }
    }

    /**
     * Returns a {@link NodeBuilder} responsible for handling new or updated nodes
     *
     * @param compiler the {@link CypherCompiler}
     * @param entity the object to save
     * @return a {@link NodeBuilder} object for either a new node or an existing one
     */
    private NodeBuilder getNodeBuilder(CypherCompiler compiler, Object entity) {

        ClassInfo classInfo = metaData.classInfo(entity);

        // transient or subclass of transient will not have class info
        if (classInfo == null) {
            return null;
        }

        CypherContext context=compiler.context();

        Object id = entityAccessStrategy.getIdentityPropertyReader(classInfo).read(entity);
        NodeBuilder nodeBuilder;
        if (id == null) {
            nodeBuilder = compiler.newNode().addLabels(classInfo.labels());
            context.registerNewObject(nodeBuilder.reference(), entity);
        } else {
            nodeBuilder = compiler.existingNode(Long.valueOf(id.toString())).addLabels(classInfo.labels());
        }
        context.visit(entity, nodeBuilder);
        logger.debug("visiting: {}", entity);
        return nodeBuilder;
    }

    /**
     * Finds all the objects that can be mapped via relationships from the object 'entity' and
     * links them in the graph.
     *
     * This includes objects that are directly linked, as well as objects linked via a relationship entity
     *
     * @param entity  the node whose relationships will be updated
     * @param nodeBuilder a {@link NodeBuilder} that knows how to create node create/update cypher phrases
     * @param horizon the depth in the tree. If this reaches 0, we stop mapping any deeper
     * @param compiler the {@link CypherCompiler}
     */
    private void mapEntityReferences(Object entity, NodeBuilder nodeBuilder, int horizon, CypherCompiler compiler) {

        logger.debug("mapping references declared by: {} ", entity);

        ClassInfo srcInfo = metaData.classInfo(entity);

        for (RelationalReader reader : entityAccessStrategy.getRelationalReaders(srcInfo)) {

            String relationshipType = reader.relationshipType();
            String relationshipDirection = reader.relationshipDirection();

            CypherContext context=compiler.context();
            Long srcIdentity = (Long) entityAccessStrategy.getIdentityPropertyReader(srcInfo).read(entity);

            logger.debug("mapping reference type: " + relationshipType);

            if (srcIdentity != null) {
                boolean cleared = clearContextRelationships(context, srcIdentity, relationshipType, relationshipDirection);
                if (!cleared) {
                    logger.debug("this relationship is already being managed: {}");
                    continue;
                }
            }

            Object relatedObject = reader.read(entity);

            if (relatedObject != null) {

                if (relatedObject instanceof Iterable) {
                    for (Object tgtObject : (Iterable<?>) relatedObject) {
                        link(tgtObject, compiler, relationshipDirection, relationshipType, srcIdentity, nodeBuilder, entity, horizon);
                    }
                } else if (relatedObject.getClass().isArray()) {
                    for (Object tgtObject : (Object[]) relatedObject) {
                        link(tgtObject, compiler, relationshipDirection, relationshipType, srcIdentity, nodeBuilder, entity, horizon);
                    }
                } else {
                    link(relatedObject, compiler, relationshipDirection, relationshipType, srcIdentity, nodeBuilder, entity, horizon);
                }
            }
        }
    }


    /**
     * Clears the relationships in the compiler context for the object represented by identity
     *
     * @param context the {@link CypherContext} for the current compiler instance
     * @param identity the id of the node at the the 'start' of the relationship
     * @param relationshipType the type of relationship
     */
    private boolean clearContextRelationships(CypherContext context, Long identity, String relationshipType, String relationshipDirection) {
        if (relationshipDirection.equals(Relationship.OUTGOING)) {
            logger.debug("context-del: ({})-[:{}]->()", identity, relationshipType);
            return context.deregisterOutgoingRelationships(identity, relationshipType);
        } else {
            logger.debug("context-del: ({})<-[:{}]-()", identity, relationshipType);
            return context.deregisterIncomingRelationships(identity, relationshipType);
        }
    }

    /**
     * Handles the requirement to link two nodes in the graph for the cypher compiler. Either node may or
     * may not already exist in the graph. The nodes at the ends of the relationships are represented
     * by source and target, but the use of these names does not imply any particular direction in the graph.
     *
     * Instead, the direction of the relationship is established between source and target by means of
     * the relationshipDirection argument.
     * 
     * In the event that the relationship being managed is represented by an instance of RelationshipEntity
     * then the target will always be a RelationshipEntity, and the actual relationship will be
     * established between the relevant start and end nodes.
     *
     * @param target         represents the node at the end of the relationship that is not represented by source
     * @param cypherCompiler     the {@link CypherCompiler}
     * @param relationshipDirection  the relationship direction to establish
     * @param relationshipType   the relationship type to establish
     * @param srcIdentity        a string representing the identity of the start node in the cypher context
     * @param nodeBuilder        a {@link NodeBuilder} that knows how to create cypher node phrases
     * @param source          represents the node at the end of the relationship that is not represented by source
     * @param horizon            the current depth we have mapped the domain model to.
     */
    private void link(Object target, CypherCompiler cypherCompiler, String relationshipDirection, String relationshipType, Long srcIdentity, NodeBuilder nodeBuilder, Object source, int horizon) {

        logger.debug("linking to entity {}", target);

        if (target != null) {
            CypherContext context = cypherCompiler.context();

            RelationshipBuilder relationshipBuilder = getRelationshipBuilder(cypherCompiler, target, relationshipDirection, relationshipType);

            if (isRelationshipEntity(target)) {
                if (!context.visitedRelationshipEntity(target)) {
                    mapRelationshipEntity(target, source, relationshipBuilder, context, nodeBuilder, cypherCompiler, horizon);
                } else {
                    logger.debug("RE already visited {}: ", target);
                }
            } else {
                mapRelatedEntity(cypherCompiler, nodeBuilder, source, srcIdentity, relationshipBuilder, target, horizon);
            }
        } else {
            logger.debug("cannot create relationship: ({})-[:{}]->(null)", srcIdentity, relationshipType);
        }
    }

    /**
     * Fetches and initialises an appropriate {@link RelationshipBuilder} for the specified relationship type
     * and direction to the supplied domain object, which may be a node or relationship in the graph.
     *
     * In the event that the domain object is a {@link RelationshipEntity}, we create a new relationship, collect
     * its properties and return a builder associated to the RE's end node instead
     *
     * @param cypherBuilder the {@link CypherCompiler}
     * @param entity  an object representing a node or relationship entity in the graph
     * @param relationshipDirection the relationship direction we want to establish
     * @param relationshipType the type of the relationship
     * @return The appropriate {@link RelationshipBuilder}
     */
    private RelationshipBuilder getRelationshipBuilder(CypherCompiler cypherBuilder, Object entity, String relationshipDirection, String relationshipType) {

        RelationshipBuilder relationshipBuilder;

        if (isRelationshipEntity(entity)) {
            Long relId = (Long) entityAccessStrategy.getIdentityPropertyReader(metaData.classInfo(entity)).read(entity);

            relationshipBuilder = relId != null
                    ? cypherBuilder.existingRelationship(relId)
                    : cypherBuilder.newRelationship();
            relationshipBuilder.type(relationshipType);
        } else {
            relationshipBuilder = cypherBuilder.newRelationship().type(relationshipType);
        }

        relationshipBuilder.direction(relationshipDirection);
        return relationshipBuilder;
    }

    /**
     * Handles the requirement to create or update a relationship in the graph from a domain object
     * that is a {@link RelationshipEntity}. Returns the the object associated with the end node of that
     * relationship in the graph.
     *
     * @param relationshipEntity the relationship entity to create or update the relationship from
     * @param relationshipBuilder a {@link RelationshipBuilder} that knows how to build cypher phrases about relationships
     * @param context the {@link CypherContext} for the compiler.
     * @return
     */
    private void mapRelationshipEntity(Object relationshipEntity, Object parent, RelationshipBuilder relationshipBuilder, CypherContext context, NodeBuilder nodeBuilder, CypherCompiler cypherCompiler, int horizon) {

        logger.debug("mapping relationshipEntity {}", relationshipEntity);

        ClassInfo relEntityClassInfo = metaData.classInfo(relationshipEntity);

        updateRelationshipEntity(context, relationshipEntity, relationshipBuilder, relEntityClassInfo);

        Object startEntity = getStartEntity(relEntityClassInfo, relationshipEntity);
        Object targetEntity = getTargetEntity(relEntityClassInfo, relationshipEntity);

        ClassInfo targetInfo = metaData.classInfo(targetEntity);
        ClassInfo startInfo = metaData.classInfo(startEntity);
        Long tgtIdentity = (Long) entityAccessStrategy.getIdentityPropertyReader(targetInfo).read(targetEntity);
        Long srcIdentity = (Long) entityAccessStrategy.getIdentityPropertyReader(startInfo).read(startEntity);

        if (mappingContext.isDirty(relationshipEntity)) {
            context.log(relationshipEntity);
            if (tgtIdentity != null && srcIdentity!=null) {
                MappedRelationship mappedRelationship = createMappedRelationship(srcIdentity, relationshipBuilder, tgtIdentity);
                if (mappingContext.mappedRelationships().remove(mappedRelationship)) {
                    logger.debug("RE successfully marked for re-writing");
                } else {
                    logger.debug("RE is new");
                }
            }
        } else {
            logger.debug("RE is new or has not changed");
        }

        if (parent == targetEntity && !context.visited(startEntity)) {
            mapRelatedEntity(cypherCompiler, nodeBuilder, targetEntity, tgtIdentity, relationshipBuilder, startEntity, horizon);
        } else if (parent == startEntity && !context.visited(targetEntity)) {
            mapRelatedEntity(cypherCompiler, nodeBuilder, startEntity, srcIdentity, relationshipBuilder, targetEntity, horizon);
        } else {
            NodeBuilder srcNodeBuilder = context.nodeBuilder(startEntity);
            NodeBuilder tgtNodeBuilder = context.nodeBuilder(targetEntity);
            updateRelationship(srcIdentity, tgtIdentity, context, srcNodeBuilder, tgtNodeBuilder, relationshipBuilder);
        }
    }

    private void updateRelationshipEntity(CypherContext context, Object relationshipEntity, RelationshipBuilder relationshipBuilder, ClassInfo relEntityClassInfo) {

        context.visitRelationshipEntity(relationshipEntity);

        AnnotationInfo annotation = relEntityClassInfo.annotationsInfo().get(RelationshipEntity.CLASS);
        if(relationshipBuilder.getType()==null) {
            relationshipBuilder.type(annotation.get(RelationshipEntity.TYPE, relEntityClassInfo.name()));
        }

        // if the RE is new, register it in the context so that we can set its ID correctly when it is created,
        if (entityAccessStrategy.getIdentityPropertyReader(relEntityClassInfo).read(relationshipEntity) == null) {
            context.registerNewObject(relationshipBuilder.getReference(), relationshipEntity);
        }

        for (PropertyReader propertyReader : entityAccessStrategy.getPropertyReaders(relEntityClassInfo)) {
            relationshipBuilder.addProperty(propertyReader.propertyName(), propertyReader.read(relationshipEntity));
        }
    }

    private Object getStartEntity(ClassInfo relEntityClassInfo, Object relationshipEntity) {
        RelationalReader actualStartNodeReader = entityAccessStrategy.getStartNodeReader(relEntityClassInfo);
        Object startEntity = actualStartNodeReader.read(relationshipEntity);
        if (startEntity == null) {
            throw new RuntimeException("@StartNode of a relationship entity may not be null");
        }
        return startEntity;
    }

    private Object getTargetEntity(ClassInfo relEntityClassInfo, Object relationshipEntity) {
        RelationalReader actualEndNodeReader = entityAccessStrategy.getEndNodeReader(relEntityClassInfo);
        Object targetEntity = actualEndNodeReader.read(relationshipEntity);
        if (targetEntity == null) {
            throw new RuntimeException("@EndNode of a relationship entity may not be null");
        }
        return targetEntity;
    }


    private MappedRelationship createMappedRelationship(Long aNode, RelationshipBuilder relationshipBuilder, Long bNode) {
        if (relationshipBuilder.hasDirection(Relationship.OUTGOING)) {
            return new MappedRelationship(aNode, relationshipBuilder.getType(), bNode);
        }  else {
            return new MappedRelationship(bNode, relationshipBuilder.getType(), aNode);
        }
    }

    /**
     * Attempts to build a simple directed relationship in the graph between
     * two objects represented as srcEntity and tgtEntity. This function recursively calls mapEntity on the
     * target entity first before attempting to create the relationship. In this way, the object graph
     * is traversed in depth-first order, and the relationships between the leaf nodes are created
     * first.
     *
     * Note that if the srcObject and tgtObject are the same, the relationship will not be created.
     *
     * @param compiler the {@link CypherCompiler}
     * @param srcNodeBuilder  a {@link NodeBuilder} that knows how to create cypher phrases about nodes
     * @param srcEntity   the domain object representing the start node of the relationship
     * @param srcIdentity  the cypher reference to the start on the object
     * @param relationshipBuilder a {@link RelationshipBuilder} that knows how to create cypher phrases about relationships
     * @param tgtEntity the domain object representing the end node of the relationship
     * @param horizon  a value representing how deep we are mapping
     */
    private void mapRelatedEntity(CypherCompiler compiler, NodeBuilder srcNodeBuilder, Object srcEntity, Long srcIdentity, RelationshipBuilder relationshipBuilder, Object tgtEntity, int horizon) {

        if (srcEntity == tgtEntity) {
            logger.debug("refusing to map an entity to itself! {} ", srcEntity);
            return;
        }

        NodeBuilder tgtNodeBuilder = mapEntity(tgtEntity, horizon, compiler);

        // tgtNodeBuilder will be null if tgtObject is a transient class, or a subclass of a transient class
        if (tgtNodeBuilder != null) {
            logger.debug("trying to map relationship between {} and {}", srcEntity, tgtEntity);
            Long tgtIdentity = (Long) entityAccessStrategy.getIdentityPropertyReader(metaData.classInfo(tgtEntity)).read(tgtEntity);
            CypherContext context = compiler.context();
            updateRelationship(srcIdentity, tgtIdentity, context, srcNodeBuilder, tgtNodeBuilder, relationshipBuilder);
        }
    }

    /**
     * Handles the requirement to update a relationship in the graph
     *
     * Two scenarios are handled :
     *
     * 1. one or more of the nodes between the relationship is new.
     * In this case, the relationship will also be new
     *
     * 2. both nodes already exist
     * In the case where the src object and tgt object both exist, we need to find out whether
     * the relationship we're considering was loaded previously, or if it has been created by the user
     * and so has not yet been persisted.
     *
     * If we have seen this relationship before we don't want to ask Neo4j to re-establish
     * it for us as it already exists, so we re-register it in the compile context. Because this relationship
     * was previously deleted from the compile context, but not from the mapping context, this brings both
     * mapping contexts into agreement about the status of this relationship, i.e. it has not changed.
     *
     * @param srcIdentity the cypher reference to the start node of the relationship
     * @param tgtIdentity  the cypher reference to the end node of the relationship
     * @param context  the {@link CypherContext} for the current statement compiler
     * @param srcNodeBuilder  a {@link NodeBuilder} that knows how to create cypher phrases about nodes
     * @param tgtNodeBuilder   a {@link NodeBuilder} that knows how to create cypher phrases about nodes
     * @param relationshipBuilder a {@link RelationshipBuilder} that knows how to create cypher phrases about relationships
     */
    private void updateRelationship(Long srcIdentity, Long tgtIdentity, CypherContext context, NodeBuilder srcNodeBuilder, NodeBuilder tgtNodeBuilder, RelationshipBuilder relationshipBuilder) {

        if (tgtIdentity == null || srcIdentity == null) {
            maybeCreateRelationship(context, srcNodeBuilder.reference(), relationshipBuilder, tgtNodeBuilder.reference());
        } else {
            MappedRelationship mappedRelationship = createMappedRelationship(srcIdentity, relationshipBuilder, tgtIdentity);
            if (!mappingContext.isRegisteredRelationship(mappedRelationship)) {
                maybeCreateRelationship(context, srcNodeBuilder.reference(), relationshipBuilder, tgtNodeBuilder.reference());
            } else {
                logger.debug("context-add: ({})-[{}:{}]->({})", mappedRelationship.getStartNodeId(), relationshipBuilder.getReference(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
                mappedRelationship.activate();
                context.registerRelationship(mappedRelationship);
            }
        }

    }
    /**
     * Checks the relationship creation request to ensure it will be handled correctly. This includes
     * ensuring the correct direction is observed, and that a new relationship (a)-[:TYPE]-(b) is created only
     * once from one of the participating nodes (rather than from both ends).
     *
     * @param context the current compiler {@link CypherContext}
     * @param src the compiler's reference to the domain object representing the start node
     * @param relationshipBuilder a {@link RelationshipBuilder} that knows how to create cypher phrases about relationships
     * @param tgt the compiler's reference to the domain object representing the end node
     */
    private void maybeCreateRelationship(CypherContext context, String src, RelationshipBuilder relationshipBuilder, String tgt) {

        if (hasTransientRelationship(context, src, relationshipBuilder.getType(), tgt)) {
            logger.debug("new relationship is already registered");
            return;
        }

        if (relationshipBuilder.hasDirection(Relationship.OUTGOING)) {
            reallyCreateRelationship(context, src, relationshipBuilder, tgt);
        } else {
            reallyCreateRelationship(context, tgt, relationshipBuilder, src);
        }
    }

    /**
     * Checks whether a new relationship request of the given type between two specified objects has
     * already been registered. The direction of the relationship is ignored. Returns true if
     * the relationship is already registered, false otherwise.
     *
     * @param ctx the current compiler {@link CypherContext}
     * @param src the compiler's reference to the domain object representing the start (or end) node
     * @param type the relationship type to check
     * @param tgt the compiler's reference to the domain object representing the end (or start) node
     * @return
     */
    private boolean hasTransientRelationship(CypherContext ctx, String src, String type, String tgt) {
        for (Object object : ctx.log()) {
            if (object instanceof TransientRelationship) {
                if (((TransientRelationship) object).equalsIgnoreDirection(src, type, tgt)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Establishes a new relationship creation request with the cypher compiler, and creates a new
     * transient relationship in the new object log.
     *
     * @param ctx the compiler {@link CypherContext}
     * @param src the compiler's reference to the domain object representing the start (or end) node
     * @param relBuilder a {@link RelationshipBuilder} that knows how to create cypher phrases about relationships
     * @param tgt the compiler's reference to the domain object representing the end (or start) node
     */
    private void reallyCreateRelationship(CypherContext ctx, String src, RelationshipBuilder relBuilder, String tgt) {

        relBuilder.relate(src, tgt);
        logger.debug("context-new: ({})-[{}:{}]->({})", src, relBuilder.getReference(), relBuilder.getType(), tgt);

        // TODO: probably needs refactoring, this is not exactly an intuitive design!
        ctx.log(new TransientRelationship(src, relBuilder.getReference(), relBuilder.getType(), tgt)); // we log the new relationship as part of the transaction context.
    }

    /**
     * Determines whether or not the given object is annotated with <code>RelationshipEntity</code> and thus
     * shouldn't be written to a node. Returns true if the object is so annotated, false otherwise
     *
     * @param potentialRelationshipEntity the domain object to check
     * @return true if the domain object is a RelationshipEntity, false otherwise
     */
    private boolean isRelationshipEntity(Object potentialRelationshipEntity) {
        ClassInfo classInfo = metaData.classInfo(potentialRelationshipEntity);
        if (classInfo == null) {
            return false;
        }
        return null != classInfo.annotationsInfo().get(RelationshipEntity.CLASS);
    }

}
