package org.springframework.data.graph.neo4j.template;

import org.hamcrest.CoreMatchers;
import org.hibernate.ejb.criteria.ParameterContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.kernel.ImpermanentGraphDatabase;
import org.springframework.dao.DataAccessException;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.data.graph.neo4j.template.Property._;

/**
 * @author mh
 * @since 19.02.11
 */
public class Neo4jTemplateApiTest {
    private Neo4jTemplate template;
    private ImpermanentGraphDatabase graphDatabase;
    private Node referenceNode;

    @Before
    public void setUp() throws Exception {
        graphDatabase = new ImpermanentGraphDatabase();
        referenceNode = graphDatabase.getReferenceNode();
        template = new Neo4jTemplate(graphDatabase);
    }

    @After
    public void tearDown() throws Exception {
        graphDatabase.shutdown();
    }

    @Test
    public void shouldExecuteCallbackInTransaction() throws Exception {
        Node refNode = template.doInTransaction(new GraphTransactionCallback<Node>() {
            @Override
            public Node doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                Node referenceNode = graph.getReferenceNode();
                referenceNode.setProperty("test", "testDoInTransaction");
                return referenceNode;
            }
        });
        assertEquals("same reference node",referenceNode,refNode);
        assertTestPropertySet(referenceNode, "testDoInTransaction");
    }

    @Test
    public void shouldRollbackTransactionOnException() {
        try {
        template.doInTransaction(new GraphTransactionCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(Status status, GraphDatabaseService graph) throws Exception {
                graph.getReferenceNode().setProperty("test","shouldRollbackTransactionOnException");
                throw new RuntimeException("please rollback");
            }
        });
        } catch(DataAccessException dae){
            //ignore
        }
        Assert.assertThat((String)graphDatabase.getReferenceNode().getProperty("test","not set"), not("shouldRollbackTransactionOnException"));
    }

    @Test
    public void shouldRollbackViaStatus() throws Exception {
        template.doInTransaction(new GraphTransactionCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(Status status, GraphDatabaseService graph) throws Exception {
                graph.getReferenceNode().setProperty("test","shouldRollbackTransactionOnException");
                status.mustRollback();
            }
        });
        Assert.assertThat((String) graphDatabase.getReferenceNode().getProperty("test","not set"), not("shouldRollbackTransactionOnException"));
    }

    // testDoInTransaction rollback
    // rollback via exception
    @Test
    public void testExecute() throws Exception {

    }

    @Test
    public void testGetReferenceNode() throws Exception {
        assertEquals(referenceNode,template.getReferenceNode());
    }

    @Test
    public void testCreateNode() throws Exception {
        Node node=template.createNode();
        assertNotNull("created node",node);
    }
    @Test
    public void testCreateNodeWithProperties() throws Exception {
        Node node=template.createNode(_("test", "testCreateNodeWithProperties"));
        assertTestPropertySet(node, "testCreateNodeWithProperties");
    }

    private void assertTestPropertySet(Node node, String testName) {
        assertEquals(testName, node.getProperty("test","not set"));
    }

    @Test
    public void testGetNode() throws Exception {

    }

    @Test
    public void testGetRelationship() throws Exception {

    }

    @Test
    public void testIndexRelationship() throws Exception {

    }

    @Test(expected = IllegalArgumentException.class)
    public void testIndexInvalidPrimitive() throws Exception {
        template.index(Mockito.mock(PropertyContainer.class),"index","field",1);
    }

    @Test
    public void testIndexNode() throws Exception {

    }

    @Test
    public void testQueryNodes() throws Exception {

    }

    @Test
    public void testRetrieveNodes() throws Exception {

    }

    @Test
    public void testQueryRelationships() throws Exception {

    }

    @Test
    public void testRetrieveRelationships() throws Exception {

    }

    @Test
    public void testTraverse() throws Exception {

    }

    @Test
    public void testTraverseOneByRelationshipTypeAndDirection() throws Exception {

    }

    @Test
    public void testTraverseOneByRelationshipTypes() throws Exception {

    }

    @Test
    public void testTraverseOneForAllRelationships() throws Exception {

    }

    @Test
    public void testRelateTo() throws Exception {

    }
}
