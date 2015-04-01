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

package org.neo4j.ogm.cypher.compiler;

import org.neo4j.ogm.cypher.statement.ParameterisedStatement;
import org.neo4j.ogm.mapper.MappedRelationship;

import java.util.*;

/**
 * Maintains contextual information throughout the process of compiling Cypher statements to persist a graph of objects.
 *
 * @author Mark Angrish
 * @author Vince Bickers
 */
public class CypherContext {

    private final Map<Object, NodeBuilder> visitedObjects = new HashMap<>();
    private final Set<Object> visitedRelationshipEntities = new HashSet<>();

    private final Map<String, Object> createdObjects = new HashMap<>();
    private final Collection<MappedRelationship> registeredRelationships = new HashSet<>();
    private final Collection<MappedRelationship> deletedRelationships = new HashSet<>();


    private final Collection<Object> log = new HashSet<>();

    private List<ParameterisedStatement> statements;

    public boolean visited(Object obj) {
        return this.visitedObjects.containsKey(obj);
    }

    public void visit(Object toPersist, NodeBuilder nodeBuilder) {
        this.visitedObjects.put(toPersist, nodeBuilder);
    }

    public void registerRelationship(MappedRelationship mappedRelationship) {
        this.registeredRelationships.add(mappedRelationship);
    }

    public NodeBuilder nodeBuilder(Object obj) {
        return this.visitedObjects.get(obj);
    }

    public boolean isRegisteredRelationship(MappedRelationship mappedRelationship) {
        return this.registeredRelationships.contains(mappedRelationship);
    }

    public void setStatements(List<ParameterisedStatement> statements) {
        this.statements = statements;
    }

    public List<ParameterisedStatement> getStatements() {
        return this.statements;
    }

    public void registerNewObject(String cypherName, Object toPersist) {
        createdObjects.put(cypherName, toPersist);
    }

    public Object getNewObject(String cypherName) {
        return createdObjects.get(cypherName);
    }

    public Collection<MappedRelationship> registeredRelationships() {
        return registeredRelationships;
    }

    public void log(Object object) {
        log.add(object);
    }

    public Collection<Object> log() {
        return log;
    }

    /**
     * Invoked when the mapper wishes to mark a set of outgoing relationships like (a)-[:T]->(*) as deleted, prior
     * to possibly re-establishing them individually as it traverses the entity graph.
     *
     * There are two reasons why a set of relationships might not be be able to be marked deleted:
     *
     * 1) the request to mark them as deleted has already been made
     * 2) the relationship is not persisted in the graph (i.e. its a new relationship)
     *
     * Only case 1) is considered to be a failed request, because this context is only concerned about
     * pre-existing relationships in the graph. In order to distinguish between the two cases, we
     * also maintain a list of successfully deleted relationships, so that uf we try to delete an already-deleted
     * set of relationships we can signal the error.
     *
     * @param src the identity of the node at the start of the relationship
     * @param relationshipType the type of the relationship
     * @return true if the relationship was deleted or doesn't exist in the graph, false otherwise
     */
    public boolean deregisterOutgoingRelationships(Long src, String relationshipType) {
        Iterator<MappedRelationship> iterator = registeredRelationships.iterator();
        boolean found = false;
        while (iterator.hasNext()) {
           MappedRelationship mappedRelationship = iterator.next();
           if (mappedRelationship.getStartNodeId() == src && mappedRelationship.getRelationshipType().equals(relationshipType)) {
               iterator.remove();
               deletedRelationships.add(mappedRelationship);
               found = true;
               //return true;
           }
        }
        if (found) return true;

        iterator = deletedRelationships.iterator();
        while (iterator.hasNext()) {
            MappedRelationship mappedRelationship = iterator.next();
            if (mappedRelationship.getStartNodeId() == src && mappedRelationship.getRelationshipType().equals(relationshipType)) {
                return false; // request already made!
            }
        }

        return true; // not deleted, but not in graph, so ok

    }

    /**
     * Invoked when the mapper wishes to mark a set of incoming relationships like (a)<-[:T]-(*) as deleted, prior
     * to possibly re-establishing them individually as it traverses the entity graph.
     *
     * There are two reasons why a set of relationships might not be be able to be marked deleted:
     *
     * 1) the request to mark them as deleted has already been made
     * 2) the relationship is not persisted in the graph (i.e. its a new relationship)
     *
     * Only case 1) is considered to be a failed request, because this context is only concerned about
     * pre-existing relationships in the graph. In order to distinguish between the two cases, we
     * also maintain a list of successfully deleted relationships, so that uf we try to delete an already-deleted
     * set of relationships we can signal the error.
     *
     * @param tgt the identity of the node at the pointy end of the relationship
     * @param relationshipType the type of the relationship
     * @return true if the relationship was deleted or doesn't exist in the graph, false otherwise
     */
    public boolean deregisterIncomingRelationships(Long tgt, String relationshipType) {
        Iterator<MappedRelationship> iterator = registeredRelationships.iterator();
        boolean found = false;
        while (iterator.hasNext()) {
            MappedRelationship mappedRelationship = iterator.next();
            if (mappedRelationship.getEndNodeId() == tgt && mappedRelationship.getRelationshipType().equals(relationshipType)) {
                iterator.remove();
                deletedRelationships.add(mappedRelationship);
                found=true;
                //return true;
            }
        }

        if (found) return true;

        iterator = deletedRelationships.iterator();
        while (iterator.hasNext()) {
            MappedRelationship mappedRelationship = iterator.next();
            if (mappedRelationship.getEndNodeId() == tgt && mappedRelationship.getRelationshipType().equals(relationshipType)) {
                return false; // request already made!
            }
        }

        return true; // not deleted, but not in graph, so ok
    }

    public void visitRelationshipEntity(Object relationshipEntity) {
        visitedRelationshipEntities.add(relationshipEntity);

    }

    public boolean visitedRelationshipEntity(Object relationshipEntity) {
        return visitedRelationshipEntities.contains(relationshipEntity);
    }
}