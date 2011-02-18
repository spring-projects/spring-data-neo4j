package org.springframework.data.graph.neo4j.template;

import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public abstract class NeoApiTest {
    protected GraphDatabaseService neo;

    @Before
    public void setUp() {
        neo = new EmbeddedGraphDatabase("target/var/neo");
    }

    @After
    public void tearDown() {
        if (neo != null) {
            clear();
            neo.shutdown();
        }
    }

    private void clear() {
        new Neo4jTemplate(neo).doInTransaction(new GraphCallback() {
            @Override
            public void doWithGraph(GraphDatabaseService graph) throws Exception {
                final DynamicRelationshipType HAS = DynamicRelationshipType.withName("HAS");
                Node startNode = graph.getReferenceNode();
                for (Node node : org.neo4j.kernel.Traversal.description().breadthFirst().relationships(HAS).traverse(startNode).nodes()) {
                    for (Relationship relationship : node.getRelationships()) {
                        relationship.delete();
                    }
                    node.delete();
                }
            }
        });
    }
}
