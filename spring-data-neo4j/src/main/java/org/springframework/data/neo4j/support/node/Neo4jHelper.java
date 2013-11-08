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

package org.springframework.data.neo4j.support.node;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.tooling.GlobalGraphOperations;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.HashMap;
import java.util.Map;

public abstract class Neo4jHelper {

    public static void cleanDb(Neo4jTemplate template) {
        cleanDb(template.getGraphDatabaseService());
    }

    public static void dumpDb(GraphDatabaseService gds) {
        final GlobalGraphOperations globalGraphOperations = GlobalGraphOperations.at(gds);
        for (Node node : globalGraphOperations.getAllNodes()) {
            System.out.println(dump(node));
        }
        for (Node node : globalGraphOperations.getAllNodes()) {
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                System.out.println(node +"-[:"+rel.getType().name() +" "+dump(rel)+"]->"+rel.getEndNode());
            }
        }
    }

    private static String dump(PropertyContainer pc) {
        final long id = pc instanceof Node ? ((Node) pc).getId() : ((Relationship) pc).getId();
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            for (String prop : pc.getPropertyKeys()) {
                props.put(prop, pc.getProperty(prop));
            }
            return String.format("(%d) %s ", id, props);
        } catch (Exception e) {
            return "(" + id + ") " + e.getMessage();
        }
    }

    public static void cleanDb(GraphDatabaseService graphDatabaseService) {
        cleanDb(graphDatabaseService, false);
    }

    public static void cleanDb( GraphDatabaseService graphDatabaseService, boolean includeReferenceNode ) {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            clearIndex(graphDatabaseService);
            removeNodes(graphDatabaseService, includeReferenceNode);
            tx.success();
        } catch(Throwable t) {
            tx.failure();
            throw new org.springframework.data.neo4j.core.UncategorizedGraphStoreException("Error cleaning database ",t);
        } finally {
            tx.finish();
        }
    }

    private static void removeNodes(GraphDatabaseService graphDatabaseService, boolean includeReferenceNode) {
        final GlobalGraphOperations globalGraphOperations = GlobalGraphOperations.at(graphDatabaseService);
        for (Node node : globalGraphOperations.getAllNodes()) {
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                try {
                    rel.delete();
                } catch(IllegalStateException ise) {
                    if (!ise.getMessage().contains("since it has already been deleted")) throw ise;
                }

            }
        }
        for (Node node : globalGraphOperations.getAllNodes()) {
            if (includeReferenceNode || !graphDatabaseService.getReferenceNode().equals(node)) {
                try {
                    node.delete();
                } catch(IllegalStateException ise) {
                    if (!ise.getMessage().contains("since it has already been deleted")) throw ise;
                }
            }
        }
    }

    private static void clearIndex(GraphDatabaseService gds) {
        IndexManager indexManager = gds.index();
        for (String ix : indexManager.nodeIndexNames()) {
            Index<Node> nodeIndex = indexManager.forNodes(ix);
            if (nodeIndex.isWriteable()) nodeIndex.delete();
        }
        for (String ix : indexManager.relationshipIndexNames()) {
            RelationshipIndex relationshipIndex = indexManager.forRelationships(ix);
            if (relationshipIndex.isWriteable()) relationshipIndex.delete();
        }
    }
}
