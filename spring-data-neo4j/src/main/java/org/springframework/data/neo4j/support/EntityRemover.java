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
package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexManager;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;

/**
* @author mh
* @since 12.10.11
*/
public class EntityRemover {

    private EntityStateHandler entityStateHandler;
    private TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy;
    private TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy;
    private IndexManager indexManager;

    public EntityRemover(EntityStateHandler entityStateHandler, TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy, TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy, IndexManager indexManager) {
        this.entityStateHandler = entityStateHandler;
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
        this.indexManager = indexManager;
    }

    public void removeNodeEntity(Object entity) {
        Node node = entityStateHandler.getPersistentState(entity, Node.class);
        if (node == null) return;
        nodeTypeRepresentationStrategy.preEntityRemoval(node);
        for (Relationship relationship : node.getRelationships()) {
            removeRelationship(relationship);
        }
        removeFromIndexes(node);
        node.delete();
    }

    public void removeRelationshipEntity(Object entity) {
        Relationship relationship = entityStateHandler.getPersistentState(entity, Relationship.class);
        if (relationship == null) return;
        removeRelationship(relationship);
    }

    private void removeRelationship(Relationship relationship) {
        relationshipTypeRepresentationStrategy.preEntityRemoval(relationship);
        removeFromIndexes(relationship);
        relationship.delete();
    }

    private void removeFromIndexes(Node node) {
        for (String indexName : indexManager.nodeIndexNames()) {
            indexManager.forNodes(indexName).remove(node);
        }
    }

    private void removeFromIndexes(Relationship relationship) {
        for (String indexName : indexManager.relationshipIndexNames()) {
            indexManager.forRelationships(indexName).remove(relationship);
        }
    }

    public void removeRelationshipTo(Object start, Object target, String type) {
        final RelationshipResult result = entityStateHandler.removeRelationshipTo(start, target, type);
        if (result!=null && result.type == RelationshipResult.Type.DELETED) {
            relationshipTypeRepresentationStrategy.preEntityRemoval(result.relationship);
        }
    }
}
