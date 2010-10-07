package org.springframework.datastore.graph.neo4j.spi.node;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

public abstract class Neo4jHelper {

    public static void cleanDb(GraphDatabaseContext graphDatabaseContext,String...indexFieldsToRemove) {
        Node refNode = graphDatabaseContext.getReferenceNode();
        for (Node node : graphDatabaseContext.getAllNodes()) {
            for (Relationship rel : node.getRelationships()) {
                rel.delete();
            }
            if (!refNode.equals(node)) {
                node.delete();
            }
        }
        for (String indexField : indexFieldsToRemove) {
            graphDatabaseContext.removeIndex(indexField);
        }
    }
}
