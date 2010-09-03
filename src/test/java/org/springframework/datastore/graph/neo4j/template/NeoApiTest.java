package org.springframework.datastore.graph.neo4j.template;

import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.datastore.graph.neo4j.template.traversal.Traversal;

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
        new NeoTemplate(neo).execute(new NeoCallback() {
            public void neo(final Status status, final Graph graph) throws Exception {
                final DynamicRelationshipType relationshipType = DynamicRelationshipType.withName("HAS");
                final Traverser allNodes = graph.traverse(Traversal.walk().both(relationshipType));
                for (Node node : allNodes) {
                    for (Relationship relationship : node.getRelationships()) {
                        relationship.delete();
                    }
                    node.delete();
                }
            }
        });
    }
}
