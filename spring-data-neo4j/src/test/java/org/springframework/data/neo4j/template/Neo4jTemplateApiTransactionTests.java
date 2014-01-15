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

package org.springframework.data.neo4j.template;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.core.NodeProxy;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.neo4j.helpers.collection.MapUtil.map;


public class Neo4jTemplateApiTransactionTests {
    private static final DynamicRelationshipType KNOWS = DynamicRelationshipType.withName("knows");
    private static final DynamicRelationshipType HAS = DynamicRelationshipType.withName("has");
    protected Neo4jTemplate template;
    protected GraphDatabase graphDatabase;
    protected Node referenceNode;
    protected Relationship relationship1;
    protected Node node1;
    protected PlatformTransactionManager transactionManager;
    protected GraphDatabaseService graphDatabaseService;


    @Before
    public void setUp() throws Exception
    {
        graphDatabaseService = createGraphDatabaseService();
        graphDatabase = createGraphDatabase();
        transactionManager = createTransactionManager();
        template = new Neo4jTemplate(graphDatabase, transactionManager);
        createData();
    }

    protected GraphDatabaseService createGraphDatabaseService() throws IOException {
        return new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    protected GraphDatabase createGraphDatabase() throws Exception {
        return new DelegatingGraphDatabase(graphDatabaseService);
    }

    @Test
    public void testBeginTxWithoutConfiguredTxManager() throws Exception {
        Neo4jTemplate template = new Neo4jTemplate(graphDatabase);
        Transaction tx = template.getGraphDatabase().beginTx();
        Node node = template.createNode();
        node.setProperty("name","foo");
        tx.success();
        tx.finish();

        tx = template.getGraphDatabase().beginTx();
        try {
            assertNotNull(node.getProperty("name"));
        } finally {
            tx.success();tx.finish();
        }
    }

    @Test
    public void testInstantiateEntity() throws Exception {
        Neo4jTemplate template = new Neo4jTemplate(graphDatabase,transactionManager);
        Transaction tx = template.getGraphDatabase().beginTx();
        try {
            Person michael = template.save(new Person("Michael", 37));
            assertNotNull(michael.getId());
        } finally {
            tx.success();tx.finish();
        }
    }

    protected PlatformTransactionManager createTransactionManager() {
        return new JtaTransactionManager(new SpringTransactionManager((GraphDatabaseAPI)graphDatabaseService));
    }

    private void createData() {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                //referenceNode = graphDatabase.getReferenceNode();
                referenceNode = graphDatabase.createNode(null);
                referenceNode.setProperty("name", "node0");
                graphDatabase.createIndex(Node.class, "node", IndexType.SIMPLE).add(referenceNode, "name", "node0");
                node1 = graphDatabase.createNode(map("name", "node1"));
                relationship1 = referenceNode.createRelationshipTo(node1, KNOWS);
                relationship1.setProperty("name", "rel1");
                graphDatabase.createIndex(Relationship.class, "relationship", IndexType.SIMPLE).add(relationship1, "name", "rel1");
            }
        });
    }

   /* @Test
    public void shouldExecuteCallbackInTransaction() throws Exception {
        Node refNode = template.exec(new GraphCallback<Node>() {
            @Override
            public Node doWithGraph(GraphDatabase graph) throws Exception {
                Node referenceNode = graphDatabase.createNode(null);
                referenceNode.setProperty("test", "testDoInTransaction");
                return referenceNode;
            }
        });
        Transaction tx = graphDatabase.beginTx();
        try {
            assertEquals("same reference node", referenceNode, refNode);
            assertTestPropertySet(referenceNode, "testDoInTransaction");
        } finally {
            tx.success();tx.finish();
        }
    }*/

    /*
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
        Transaction tx = graphDatabase.beginTx();
        try {
            Assert.assertThat((String) graphDatabase.getReferenceNode().getProperty("test", "not set"), not("shouldRollbackTransactionOnException"));
        } finally {
            tx.success();tx.finish();
        }
    }  */

    /*
    @Test
    public void shouldRollbackViaStatus() throws Exception {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
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
        Transaction tx = graphDatabase.beginTx();
        try {
            Assert.assertThat((String) graphDatabase.getReferenceNode().getProperty("test", "not set"), not("shouldRollbackTransactionOnException"));
        } finally {
            tx.success();tx.finish();
        }
    }    */

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
    @Ignore
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
        Neo4jTemplate template = new Neo4jTemplate(graphDatabase, transactionManager);
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                graph.getNodeById( Long.MAX_VALUE );
            }
        });
    }

    /*@Test
    public void shouldExecuteCallback() throws Exception {
        Long refNodeId = template.exec(new GraphCallback<Long>() {
            @Override
            public Long doWithGraph(GraphDatabase graph) throws Exception {
                return graph.getReferenceNode().getId();
            }
        });
        Transaction tx = graphDatabase.beginTx();
        try {
            assertEquals(referenceNode.getId(), (long) refNodeId);
        } finally {
            tx.success();tx.finish();
        }
    }*/

    @Test
    public void testCreateNode() throws Exception {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Node node = template.createNode(null);
                assertNotNull("created node", node);
            }
        });
    }

    @Test
    public void testCreateNodeWithProperties() throws Exception {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Node node = template.createNode(map("test", "testCreateNodeWithProperties"));
                assertTestPropertySet(node, "testCreateNodeWithProperties");
            }
        });
    }

    private void assertTestPropertySet(Node node, String testName) {
        assertEquals(testName, node.getProperty("test","not set"));
    }
}
