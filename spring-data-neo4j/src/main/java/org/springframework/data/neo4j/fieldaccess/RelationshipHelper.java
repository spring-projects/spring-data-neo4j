/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * @author mh
 * @since 28.02.12
 */
public class RelationshipHelper {

    private final Neo4jTemplate template;
    private final Direction direction;
    private final RelationshipType type;

    public RelationshipHelper(Neo4jTemplate template, Direction direction, RelationshipType type) {
        this.template = template;
        this.direction = direction;
        this.type = type;
    }

    private Iterable<Node> getOtherNodes(Node node) {
        final Set<Node> result = new HashSet<Node>();
        for (final Relationship rel : node.getRelationships(type, direction)) {
            result.add(rel.getOtherNode(node));
        }
        return result;
    }

    protected Relationship obtainSingleRelationship(final Node start, final Node end) {
        final Iterable<Relationship> existingRelationships = start.getRelationships(type, direction);
        for (final Relationship existingRelationship : existingRelationships) {
            if (existingRelationship != null && existingRelationship.getOtherNode(start).equals(end))
                return existingRelationship;
        }
        return start.createRelationshipTo(end, type);
    }

    protected Node checkAndGetNode(Object entity) {
        if (entity == null) throw new IllegalStateException("Entity is null");
        Node node = getNode(entity);
        if (node != null) return node;
        throw new IllegalStateException("Entity must have a backing Node");
    }

    protected void removeMissingRelationshipsInStoreAndKeepOnlyNewRelationShipsInSet(Node node, Set<Node> targetNodes) {
        for (Relationship relationship : node.getRelationships(type, direction)) {
            if (!targetNodes.remove(relationship.getOtherNode(node)))
                relationship.delete();
        }
    }

    protected void createAddedRelationships(Node node, Set<Node> targetNodes) {
        for (Node targetNode : targetNodes) {
            createSingleRelationship(node, targetNode);
        }
    }

    // adding cascade
    @SuppressWarnings("unchecked")
    protected Set<Node> createSetOfTargetNodes(Object newVal, final Class<?> relatedType) {
        if (!(newVal instanceof Set)) {
            throw new IllegalArgumentException("New value must be a Set, was: " + newVal.getClass());
        }
        Set<Node> nodes = new HashSet<Node>();
        for (Object value : (Set<Object>) newVal) {
            if (!relatedType.isInstance(value)) {
                throw new IllegalArgumentException("New value elements must be " + relatedType);
            }
            nodes.add(getOrCreateState(value));
        }
        return nodes;
    }

    protected Node getOrCreateState(Object value) {
        final Node Node = getNode(value);
        if (Node != null) return Node;
        final Object saved = template.save(value);
        final Node newState = getNode(saved);
        Assert.notNull(newState);
        return newState;
    }


    protected Set<Object> createEntitySetFromRelationshipEndNodes(Object entity, final MappingPolicy mappingPolicy, final Class<?> relatedType) {
        final Iterable<Node> nodes = getStatesFromEntity(entity);
        final Set<Object> result = new HashSet<Object>();
        for (final Node otherNode : nodes) {
            Object target = template.createEntityFromState(otherNode, relatedType, mappingPolicy);
            result.add(target);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected Relationship createSingleRelationship(Node start, Node end) {
        if (end == null) return null;
        switch (direction) {
            case OUTGOING:
            case BOTH: { // TODO both should actually check in both directions, perhaps have the obtain method get the direction instead and figure out what to do itself
                return obtainSingleRelationship(start, end);
            }
            case INCOMING:
                return obtainSingleRelationship(end, start);
            default:
                throw new InvalidDataAccessApiUsageException("invalid direction " + direction);
        }
    }

    protected Iterable<Node> getStatesFromEntity(final Object entity) {
        final Node node = getNode(entity);
        return getOtherNodes(node);
    }


    protected Node getNode(final Object entity) {
        return template.getPersistentState(entity);
    }

    public Iterable<Relationship> getRelationships(Node node) {
        return node.getRelationships(type, direction);
    }

    public Relationship getSingleRelationship(Node node) {
        return node.getSingleRelationship(type,direction);
    }
}
