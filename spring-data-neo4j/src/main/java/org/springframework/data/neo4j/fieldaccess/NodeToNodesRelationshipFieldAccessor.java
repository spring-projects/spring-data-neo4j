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
import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public abstract class NodeToNodesRelationshipFieldAccessor<TARGET extends GraphBacked> extends AbstractNodeRelationshipFieldAccessor<NodeBacked, Node, TARGET, Node> {
    public NodeToNodesRelationshipFieldAccessor(final Class<? extends TARGET> clazz, final GraphDatabaseContext graphDatabaseContext, final Direction direction, final RelationshipType type, Field field) {
        super(clazz, graphDatabaseContext, direction, type,field);
    }

    @Override
    protected Relationship obtainSingleRelationship(final Node start, final Node end) {
        final Iterable<Relationship> existingRelationships = start.getRelationships(type, direction);
        for (final Relationship existingRelationship : existingRelationships) {
            if (existingRelationship!=null && existingRelationship.getOtherNode(start).equals(end)) return existingRelationship;
        }
        return start.createRelationshipTo(end, type);
    }

    @Override
    protected Iterable<Node> getStatesFromEntity(final NodeBacked entity) {
        final Node entityNode = getState(entity);
        final Set<Node> result = new HashSet<Node>();
        for (final Relationship rel : entityNode.getRelationships(type, direction)) {
            result.add(rel.getOtherNode(entityNode));
		}
        return result;
    }

    @Override
    protected Node getState(final NodeBacked entity) {
        return entity.getPersistentState();
    }
}
