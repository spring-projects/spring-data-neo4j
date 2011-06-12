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

package org.springframework.data.graph.neo4j.template;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.graph.core.GraphDatabase;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.graph.neo4j.support.path.PathMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.springframework.data.graph.core.Property._;


public class Neo4jTemplateApiTest {
    private static final DynamicRelationshipType KNOWS = DynamicRelationshipType.withName("knows");
    private static final DynamicRelationshipType HAS = DynamicRelationshipType.withName("has");
    protected Neo4jTemplate template;
    protected static GraphDatabase graphDatabase;
    protected Node referenceNode;
    protected Relationship relationship1;
    protected Node node1;
    protected static PlatformTransactionManager transactionManager;
    protected static GraphDatabaseService graphDatabaseService;



    @Before
    public void setUp() throws Exception
    {
        graphDatabaseService = createGraphDatabaseService();
        graphDatabase = createGraphDatabase();
        transactionManager = createTransactionManager();
        referenceNode = graphDatabase.getReferenceNode();
        template = new Neo4jTemplate(graphDatabase, transactionManager);
        createData();
    }

    protected GraphDatabaseService createGraphDatabaseService() throws IOException {
        return new ImpermanentGraphDatabase();
    }

    protected GraphDatabase createGraphDatabase() throws Exception {
        return new DelegatingGraphDatabase(graphDatabaseService);
    }

    protected PlatformTransactionManager createTransactionManager() {
        return new JtaTransactionManager(new SpringTransactionManager(graphDatabaseService));
    }

    private void createData() {

        new TransactionTemplate(Neo4jTemplateApiTest.transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                referenceNode.setProperty("name", "node0");
                graphDatabase.createIndex(Node.class, "node", false).add(referenceNode, "name", "node0");
                node1 = graphDatabase.createNode(_("name", "node1"));
                relationship1 = referenceNode.createRelationshipTo(node1, KNOWS);
                relationship1.setProperty("name", "rel1");
                graphDatabase.createIndex(Relationship.class, "relationship", false).add(relationship1, "name", "rel1");
            }
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (graphDatabaseService!=null) {
            graphDatabaseService.shutdown();
        }
    }

    @Test
    public void shouldExecuteCallbackInTransaction() throws Exception {
        Node refNode = template.exec(new GraphCallback<Node>() {
            @Override
            public Node doWithGraph(GraphDatabase graph) throws Exception {
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
            template.exec(new GraphCallback.WithoutResult() {
                @Override
                public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                    graph.getReferenceNode().setProperty("test", "shouldRollbackTransactionOnException");
                    throw new RuntimeException("please rollback");
                }
            });
        } catch(RuntimeException re){
            //ignore
        }
        Assert.assertThat((String)graphDatabase.getReferenceNode().getProperty("test","not set"), not("shouldRollbackTransactionOnException"));
    }

    @Test
    public void shouldRollbackViaStatus() throws Exception {
        new TransactionTemplate(Neo4jTemplateApiTest.transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                template.exec(new GraphCallback.WithoutResult() {
                    @Override
                    public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                        graph.getReferenceNode().setProperty("test", "shouldRollbackTransactionOnException");
                        status.setRollbackOnly();
                    }
                });
            }
        });
        Assert.assertThat((String) graphDatabase.getReferenceNode().getProperty("test","not set"), not("shouldRollbackTransactionOnException"));
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotConvertUserRuntimeExceptionToDataAccessException() {
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                throw new RuntimeException();
            }
        });
    }

    @Test(expected = DataAccessException.class)
    public void shouldConvertMissingTransactionExceptionToDataAccessException() {
        Neo4jTemplate template = new Neo4jTemplate(graphDatabase, null);
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                graph.createNode(null);
            }
        });
    }
    @Test(expected = DataAccessException.class)
    public void shouldConvertNotFoundExceptionToDataAccessException() {
        Neo4jTemplate template = new Neo4jTemplate(graphDatabase, Neo4jTemplateApiTest.transactionManager);
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                graph.getNodeById( Long.MAX_VALUE );
            }
        });
    }
    @Test(expected = DataAccessException.class)
    public void shouldConvertTemplateNotFoundExceptionToDataAccessException() {
        template.getNode(Long.MAX_VALUE);
    }

    @Test
    public void shouldExecuteCallback() throws Exception {
        Long refNodeId = template.exec(new GraphCallback<Long>() {
            @Override
            public Long doWithGraph(GraphDatabase graph) throws Exception {
                return graph.getReferenceNode().getId();
            }
        });
        assertEquals(referenceNode.getId(),(long)refNodeId);
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
        Node lookedUpNode = template.getNode(referenceNode.getId());
        assertEquals(referenceNode,lookedUpNode);
    }

    @Test
    public void testGetRelationship() throws Exception {
        Relationship lookedUpRelationship = template.getRelationship(relationship1.getId());
        assertThat(lookedUpRelationship,is(relationship1));

    }

    @Test
    public void testIndexRelationship() throws Exception {
        Index<Relationship> index = graphDatabase.getIndex("relationship");
        Relationship lookedUpRelationship = index.get( "name", "rel1" ).getSingle();
        assertThat("same relationship from index",lookedUpRelationship,is(relationship1));
    }

    @Test
    public void testIndexNode() throws Exception {
        template.index("node", node1, "name","node1");
        Index<Node> index = graphDatabase.getIndex("node");
        Node lookedUpNode= index.get( "name", "node1" ).getSingle();
        assertThat("same node from index",lookedUpNode,is(node1));
    }

    @Test
    public void testQueryNodes() throws Exception {
        assertSingleResult("node0", template.query("node", new NodeNameMapper(), new TermQuery(new Term("name", "node0"))));
    }

    @Test
    public void testRetrieveNodes() throws Exception {
        assertSingleResult("node0", template.query("node", new NodeNameMapper(), "name", "node0"));
    }

    @Test
    public void testQueryRelationships() throws Exception {
        assertSingleResult("rel1", template.query("relationship", new RelationshipNameMapper(), new TermQuery(new Term("name", "rel1"))));
    }

    @Test
    public void testRetrieveRelationships() throws Exception {
        assertSingleResult("rel1",template.query("relationship", new RelationshipNameMapper(), "name", "rel1"));
    }

    @Test
    public void testTraverse() throws Exception {
        assertSingleResult("node1",template.traverseGraph(referenceNode, new NodeNameMapper(), Traversal.description().relationships(KNOWS).prune(Traversal.pruneAfterDepth(1)).filter(Traversal.returnAllButStartNode())));
    }

    @Test
    public void shouldGetDirectRelationship() throws Exception {
        assertSingleResult("rel1", template.traverseNext(referenceNode, new RelationshipNameMapper()));
    }
    @Test
    public void shouldGetDirectRelationshipForType() throws Exception {
        assertSingleResult("rel1", template.traverseNext(referenceNode, new RelationshipNameMapper(), KNOWS));
    }
    @Test
    public void shouldGetDirectRelationshipForTypeAndDirection() throws Exception {
        assertSingleResult("rel1", template.traverseNext(referenceNode, new RelationshipNameMapper(), KNOWS, Direction.OUTGOING));
    }

    private <T> void assertSingleResult(T expected, Iterable<T> iterable) {
        Iterator<T> result = iterable.iterator();
        assertEquals(expected, result.next());
        assertEquals(false, result.hasNext());
    }


    @Test
    public void shouldCreateRelationshipWithProperty() throws Exception {
        Relationship relationship = template.createRelationship(referenceNode, node1, HAS,_("name","rel2"));
        assertNotNull(relationship);
        assertEquals(referenceNode, relationship.getStartNode());
        assertEquals(node1,relationship.getEndNode());
        assertEquals(HAS.name(), relationship.getType().name());
        assertEquals("rel2",relationship.getProperty("name","not set"));
    }

    private static class RelationshipNameMapper implements PathMapper<String> {
        @Override
        public String mapPath(Path path) {
            return (String) path.lastRelationship().getProperty("name","not set");
        }
    }
    private static class NodeNameMapper implements PathMapper<String> {
        @Override
        public String mapPath(Path path) {
            return (String) path.endNode().getProperty("name","not set");
        }
    }
}
