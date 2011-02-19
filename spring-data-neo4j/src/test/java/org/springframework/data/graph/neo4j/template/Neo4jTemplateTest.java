package org.springframework.data.graph.neo4j.template;

import org.junit.Test;
import org.neo4j.graphdb.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.data.graph.neo4j.template.Neo4jTemplateTest.Type.HAS;

public class Neo4jTemplateTest extends NeoApiTest {

    enum Type implements RelationshipType {
        HAS
    }

    @Test
    public void testRefNode() {
        new Neo4jTemplate(neo).doInTransaction(new TransactionGraphCallback() {
            public void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                Node refNode = graph.getReferenceNode();
                Node refNodeById = graph.getNodeById(refNode.getId());
                assertEquals("same ref node", refNode, refNodeById);
            }
        });
    }

    @Test
    public void testSingleNode() {
        final Neo4jTemplate template = new Neo4jTemplate(neo);
        template.doInTransaction(new TransactionGraphCallback() {
            public void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                Node refNode = graph.getReferenceNode();
                // TODO easy API Node node = graph.createNode(Property._("name", "Test"), Property._("size", 100));
                Node node = graph.createNode();
                node.setProperty("name", "Test");
                node.setProperty("size", 100);
                refNode.createRelationshipTo(node, HAS);

                final Relationship toTestNode = refNode.getSingleRelationship(HAS, Direction.OUTGOING);
                final Node nodeByRelationship = toTestNode.getEndNode();
                assertEquals("Test", nodeByRelationship.getProperty("name"));
                assertEquals(100, nodeByRelationship.getProperty("size"));
            }
        });
        template.doInTransaction(new TransactionGraphCallback() {
            public void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                Node refNode = graph.getReferenceNode();
                final Relationship toTestNode = refNode.getSingleRelationship(HAS, Direction.OUTGOING);
                final Node nodeByRelationship = toTestNode.getEndNode();
                assertEquals("Test", nodeByRelationship.getProperty("name"));
                assertEquals(100, nodeByRelationship.getProperty("size"));
            }
        });
    }

    @Test
    public void testRollback() {
        final Neo4jTemplate template = new Neo4jTemplate(neo);
        template.doInTransaction(new TransactionGraphCallback() {
            @Override
            public void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                Node node = graph.getReferenceNode();
                node.setProperty("test", "test");
                assertEquals("test", node.getProperty("test"));
                status.mustRollback();
            }
        });
        template.execute(new GraphCallback() {
            public void doWithGraph(final GraphDatabaseService graph) throws Exception {
                Node node = graph.getReferenceNode();
                assertFalse(node.hasProperty("test"));
            }
        });
    }
}
