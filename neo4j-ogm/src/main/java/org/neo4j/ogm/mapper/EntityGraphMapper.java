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

import java.util.Collection;
import java.util.Iterator;

/**
 * Implementation of {@link EntityToGraphMapper} that is driven by an instance of {@link MetaData}.
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

    /**
     *
     */
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

        logger.info("context initialised with {} relationships", mappingContext.mappedRelationships().size());

        //compiler.context().registeredRelationships().addAll(mappingContext.mappedRelationships());

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
                //mappedRelationship.deactivate();
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

        if (isRelationshipEntity(entity)) {
            throw new RuntimeException("Should not happen!");
        }

        CypherContext context=compiler.context();

        if (context.visited(entity)) {
            return context.retrieveNodeBuilderForObject(entity);
        }

        NodeBuilder nodeBuilder = getNodeBuilder(compiler, entity);
        if (nodeBuilder != null) {
            update(entity, context, nodeBuilder);
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
    private void update(Object entity, CypherContext context, NodeBuilder nodeBuilder) {
        if (mappingContext.isDirty(entity)) {
            context.log(entity);
            ClassInfo classInfo = metaData.classInfo(entity);
            nodeBuilder.mapProperties(entity, classInfo, entityAccessStrategy);
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
        if (id == null) {
            NodeBuilder newNode = compiler.newNode().addLabels(classInfo.labels());
            context.visit(entity, newNode);
            context.registerNewObject(newNode.reference(), entity);
            return newNode;
        }
        NodeBuilder existingNode = compiler.existingNode(Long.valueOf(id.toString())).addLabels(classInfo.labels());
        context.visit(entity, existingNode);

        return existingNode;
    }

    /**
     * Finds all the objects that can be mapped via relationships from the object 'srcObject' and
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

        CypherContext context=compiler.context();

        ClassInfo srcInfo = metaData.classInfo(entity);
        Long srcIdentity = (Long) entityAccessStrategy.getIdentityPropertyReader(srcInfo).read(entity);

        for (RelationalReader reader : entityAccessStrategy.getRelationalReaders(srcInfo)) {

            Object relatedObject = reader.read(entity);

            String relationshipType = reader.relationshipType();
            String relationshipDirection = reader.relationshipDirection();

            clearContextRelationships(context, srcIdentity, relationshipType, relationshipDirection);

            if (relatedObject instanceof Iterable) {
                logger.debug("mapping iterable of size {}", ((Collection<?>) relatedObject).size());
                for (Object tgtObject : (Iterable<?>) relatedObject) {
                    logger.debug("mapping iterable entity: {} ", relatedObject);
                    link(tgtObject, compiler, relationshipDirection, relationshipType, srcIdentity, nodeBuilder, entity, horizon);
                }
            } else {
                logger.debug("mapping non-iterable entity {}", relatedObject);
                link(relatedObject, compiler, relationshipDirection, relationshipType, srcIdentity, nodeBuilder, entity, horizon);
            }
        }
    }

    /**
     * Clears the relationships in the compiler context for the object represented by srcIdentity
     *
     * @param context the {@link CypherContext} for the current compiler instance
     * @param identity the id of the node at the the 'start' of the relationship
     * @param relationshipType the type of relationship
     */
    private void clearContextRelationships(CypherContext context, Long identity, String relationshipType, String relationshipDirection) {
        if (identity != null) {
            if (relationshipDirection.equals(Relationship.OUTGOING)) {
                logger.info("context-del: ({})-[:{}]->()", identity, relationshipType);
                context.deregisterOutgoingRelationships(identity, relationshipType);
            } else {
                logger.info("context-del: ()-[:{}]->({})", relationshipType, identity);
                context.deregisterIncomingRelationships(identity, relationshipType);
            }
        }
    }

    /**
     * Handles the requirement to link two nodes in the graph for the cypher compiler. The two nodes may or
     * may not already exist in the graph. The nodes at the ends of the relationships are represented
     * by srcObject and tgtObject, but the use of these names does not imply any particular direction.
     *
     * @param targetEntity         represents the node at the end of the relationship that is not represented by srcObject
     * @param cypherCompiler     the {@link CypherCompiler}
     * @param relationshipDirection  the relationship direction to establish
     * @param relationshipType   the relationship type to establish
     * @param srcIdentity        a string representing the identity of the start node in the cypher context
     * @param nodeBuilder        a {@link NodeBuilder} that knows how to create cypher node phrases
     * @param sourceEntity          represents the node at the end of the relationship that is not represented by srcObject
     * @param horizon            the current depth we have mapped the domain model to.
     */
    private void link(Object targetEntity, CypherCompiler cypherCompiler, String relationshipDirection, String relationshipType, Long srcIdentity, NodeBuilder nodeBuilder, Object sourceEntity, int horizon) {
        if (targetEntity != null) {
            CypherContext context = cypherCompiler.context();
            RelationshipBuilder relationship = getRelationshipBuilder(cypherCompiler, targetEntity, relationshipDirection, relationshipType);
            if (isRelationshipEntity(targetEntity)) {
                targetEntity = mapRelationshipEntity(srcIdentity, targetEntity, relationship, context);
            }
            mapRelatedEntity(cypherCompiler, nodeBuilder, sourceEntity, srcIdentity, relationship, targetEntity, horizon);
        } else {
            logger.warn("cannot create relationship: ({})-[:{}]->(null)", srcIdentity, relationshipType);
        }
    }

    /**
     * Fetches and initialises an appropriate {@link RelationshipBuilder} for the specified relationship type
     * and direction to the supplied domain object, which may be a node or relationship in the graph.
     *
     * In the event that the domain object is a {@link RelationshipEntity}, we create a new relationship, collect
     * its properties and return a a builder associated to the RE's end node instead
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
            relationshipBuilder.type(relationshipType); //Should this be set only if relId==null?
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
     * @param srcIdentity the id of the node at the start of the relationship in the graph
     * @param relationshipEntity the relationship entity to create or update the relationship from
     * @param relationshipBuilder a {@link RelationshipBuilder} that knows how to build cypher phrases about relationships
     * @param context the {@link CypherContext} for the compiler.
     * @return
     */
    private Object mapRelationshipEntity(Long srcIdentity, Object relationshipEntity, RelationshipBuilder relationshipBuilder, CypherContext context) {

        ClassInfo relEntityClassInfo = metaData.classInfo(relationshipEntity);

        logger.debug("mapping relationshipEntity {}", relEntityClassInfo.name());

        AnnotationInfo annotation = relEntityClassInfo.annotationsInfo().get(RelationshipEntity.CLASS);
        if(relationshipBuilder.getType()==null) {
            relationshipBuilder.type(annotation.get(RelationshipEntity.TYPE, relEntityClassInfo.name()));
        }

        // if the RE is new, register it in the context so that we can
        // set its ID correctly when it is created,
        if (entityAccessStrategy.getIdentityPropertyReader(relEntityClassInfo).read(relationshipEntity) == null) {
            context.registerNewObject(relationshipBuilder.getReference(), relationshipEntity);
        }

        for (PropertyReader propertyReader : entityAccessStrategy.getPropertyReaders(relEntityClassInfo)) {
            relationshipBuilder.addProperty(propertyReader.propertyName(), propertyReader.read(relationshipEntity));
        }

        RelationalReader actualEndNodeReader = entityAccessStrategy.getEndNodeReader(relEntityClassInfo);
        Object targetEntity = actualEndNodeReader.read(relationshipEntity);

        if (targetEntity == null) {
            throw new RuntimeException("@EndNode of a relationship entity may not be null");
        }

        if (mappingContext.isDirty(relationshipEntity)) {
            ClassInfo targetInfo = metaData.classInfo(targetEntity);
            Long tgtIdentity = (Long) entityAccessStrategy.getIdentityPropertyReader(targetInfo).read(targetEntity);
            if (tgtIdentity != null && srcIdentity!=null) {
                logger.debug("RE in the database is stale");
                MappedRelationship mappedRelationship = createMappedRelationship(srcIdentity, relationshipBuilder, tgtIdentity);
                if (mappingContext.mappedRelationships().remove(mappedRelationship)) {
                    logger.debug("RE successfully marked for re-writing");
                } else {
                    logger.warn("Could not find RE in mappingContext");
                }
            }
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
     * Registers the requirement to build a simple directed relationship in the graph between
     * two objects represented as srcObject and tgtObject.
     *
     * If the srcObject and tgtObject are the same, the relationship will not be created.
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
            logger.warn("refusing to map an entity to itself! {} ", srcEntity);
            return;
        }

        NodeBuilder target = mapEntity(tgtEntity, horizon, compiler);

        // target will be null if tgtObject is a transient class, or a subclass of a transient class
        if (target != null) {

            logger.debug("trying to map relationship between {} and {}", srcEntity, tgtEntity);

            CypherContext context=compiler.context();
            Long tgtIdentity = (Long) entityAccessStrategy.getIdentityPropertyReader(metaData.classInfo(tgtEntity)).read(tgtEntity);

            // this relationship is new, because the src object or tgt object has not yet been persisted
            if (tgtIdentity == null || srcIdentity == null) {
                logger.debug("({})-[:{}]->({}) is not registered in mappingContext", srcNodeBuilder.reference(), relationshipBuilder.getType(), target.reference());
                maybeCreateRelationship(context, srcNodeBuilder.reference(), relationshipBuilder, target.reference());
            } else {
                // in the case where the src object and tgt object both exist, we need to find out whether
                // the relationship we're considering was loaded previously, or if it has been created by the user
                // and so has not yet been persisted.

                MappedRelationship mappedRelationship = createMappedRelationship(srcIdentity, relationshipBuilder, tgtIdentity);
                if (!mappingContext.isRegisteredRelationship(mappedRelationship)) {
                    logger.info("({})-[:{}]->({}) is not registered in mappingContext", mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
                    maybeCreateRelationship(context, srcNodeBuilder.reference(), relationshipBuilder, target.reference());
                } else {
                    // we have seen this relationship before and we don't want to ask Neo4j to re-establish
                    // it for us as it already exists, so we register it in the tx context. Because this relationship
                    // was previously deleted from the tx context, but not from the mapping context, this brings both
                    // mapping contexts into agreement about the status of this relationship, i.e. it has not changed.
                    logger.info("context-add: ({})-[:{}]->({})", mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
                    // it may have been deactivated by a previous delete, which has now been reversed.
                    mappedRelationship.activate();
                    context.registerRelationship(mappedRelationship);
                }
            }
        }
    }

    /**
     * Checks the relationship creation request to ensure it will be handled correctly. This includes
     * ensuring the correct direction is observed, and that a relationship with direction UNDIRECTED is created only
     * once from one of the participating nodes (rather than from both ends).
     *
     * @param context the current compiler {@link CypherContext}
     * @param src the compiler's reference to the domain object representing the start node
     * @param relationshipBuilder a {@link RelationshipBuilder} that knows how to create cypher phrases about relationships
     * @param tgt the compiler's reference to the domain object representing the end node
     */
    private void maybeCreateRelationship(CypherContext context, String src, RelationshipBuilder relationshipBuilder, String tgt) {
        if (relationshipBuilder.hasDirection(Relationship.UNDIRECTED)) {
            if (hasTransientRelationship(context, src, relationshipBuilder.getType(), tgt)) {
                return;
            }
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
        logger.info("context-new: ({})-[:{}]->({})", src, relBuilder.getType(), tgt);
        relBuilder.relate(src, tgt);
        // TODO: probably needs refactoring, this is not exactly an intuitive design!
        ctx.log(new TransientRelationship(src, relBuilder.getType(), tgt)); // we log the new relationship as part of the transaction context.
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
