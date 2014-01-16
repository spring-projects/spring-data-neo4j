package org.neo4j.cineasts.service;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexManager;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 02.03.11
 */
public class Neo4jDatabaseCleaner {
    private GraphDatabaseService graph;

    public Neo4jDatabaseCleaner(Neo4jOperations template) {
        this.graph = ((DelegatingGraphDatabase)template.getGraphDatabase()).getGraphDatabaseService();
    }

    public Map<String, Object> cleanDb() {
        Map<String, Object> result = new HashMap<String, Object>();
        Transaction tx = graph.beginTx();
        try {
            removeNodes(result);
            clearIndex(result);
            tx.success();
        } finally {
            tx.close();
        }
        return result;
    }

    private void removeNodes(Map<String, Object> result) {
        Node refNode = graph.getReferenceNode();
        int nodes = 0, relationships = 0;
        for (Node node : graph.getAllNodes()) {
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                rel.delete();
                relationships++;
            }
            if (!refNode.equals(node)) {
                node.delete();
                nodes++;
            }
        }
        result.put("nodes", nodes);
        result.put("relationships", relationships);

    }

    private void clearIndex(Map<String, Object> result) {
        IndexManager indexManager = graph.index();
        result.put("node-indexes", Arrays.asList(indexManager.nodeIndexNames()));
        result.put("relationship-indexes", Arrays.asList(indexManager.relationshipIndexNames()));
        for (String ix : indexManager.nodeIndexNames()) {
            indexManager.forNodes(ix).delete();
        }
        for (String ix : indexManager.relationshipIndexNames()) {
            indexManager.forRelationships(ix).delete();
        }
    }
}
