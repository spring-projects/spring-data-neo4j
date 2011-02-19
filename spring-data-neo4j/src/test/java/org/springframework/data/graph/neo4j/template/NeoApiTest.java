package org.springframework.data.graph.neo4j.template;

import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public abstract class NeoApiTest {
    protected GraphDatabaseService neo;

    @Before
    public void setUp() {
        neo = new EmbeddedGraphDatabase("target/template-db");
    }

    @After
    public void tearDown() {
        if (neo != null) {
            clear();
            neo.shutdown();
        }
    }

    private void clear() {
        try {
        new Neo4jTemplate(neo).doInTransaction(new TransactionGraphCallback() {
            public void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
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
            }
        });
        } catch(Exception e) {
            e.printStackTrace();
            // ignore
        }
    }
}
