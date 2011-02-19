package org.springframework.data.graph.neo4j.template;

import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public abstract class NeoApiTest {
    protected GraphDatabaseService graph;

    @Before
    public void setUp() {
        graph = new EmbeddedGraphDatabase("target/template-db");
    }

    @After
    public void tearDown() {
        if (graph != null) {
            clear();
            graph.shutdown();
        }
    }

    private void clear() {
        try {
        new Neo4jTemplate(graph).doInTransaction(new GraphTransactionCallback<Void>() {
            public Void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                for (Node node : graph.getAllNodes()) {
                    for (Relationship relationship : node.getRelationships()) {
                        relationship.delete();
                    }
                }
                Node referenceNode = graph.getReferenceNode();
                for (Node node : graph.getAllNodes()) {
                    if (node.equals(referenceNode)) continue;
                    node.delete();
                }
                return null;
            }
        });
        } catch(Exception e) {
            e.printStackTrace();
            // ignore
        }
    }
}
