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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Iterator;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.neo4j.helpers.collection.MapUtil.map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:template-config-context.xml"})
public class FullNeo4jTemplateTests {
    private static final DynamicRelationshipType KNOWS = DynamicRelationshipType.withName("knows");
    private static final DynamicRelationshipType HAS = DynamicRelationshipType.withName("has");
    @Autowired
    Neo4jTemplate neo4jTemplate;
    @Autowired
    protected GraphDatabase graphDatabase;
    protected Node node0;
    protected Relationship relationship1;
    protected Node node1;
    @Autowired
    PlatformTransactionManager neo4jTransactionManager;


    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }

    @Before
    public void setUp() throws Exception {
        Transaction tx = neo4jTemplate.getGraphDatabase().beginTx();
        try {
            Neo4jHelper.cleanDb(neo4jTemplate);
            tx.success();
        } finally {
            tx.close();
        }
        tx = neo4jTemplate.getGraphDatabase().beginTx();
        try {
            createData();
            tx.success();
        } finally {
            tx.close();
        }
    }

    private void createData() {

        new TransactionTemplate(neo4jTransactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                node0 = graphDatabase.createNode(map("name", "node0"), asList("Node"));
                graphDatabase.createIndex(Node.class, "node", IndexType.SIMPLE).add(node0, "name", "node0");
                node1 = graphDatabase.createNode(map("name", "node1"), asList("Node"));
                relationship1 = node0.createRelationshipTo(node1, KNOWS);
                relationship1.setProperty("name", "rel1");
                graphDatabase.createIndex(Relationship.class, "relationship", IndexType.SIMPLE).add(relationship1, "name", "rel1");
            }
        });
    }

    @Test
    public void shouldExecuteCallbackInTransaction() throws Exception {
        Node refNode = neo4jTemplate.exec(new GraphCallback<Node>() {
            @Override
            public Node doWithGraph(GraphDatabase graph) throws Exception {
                Node referenceNode = graph.getNodeById(node0.getId());
                referenceNode.setProperty("test", "testDoInTransaction");
                return referenceNode;
            }
        });
        Transaction tx=graphDatabase.beginTx();
        try {
            assertEquals("same reference node", node0, refNode);
            assertTestPropertySet(node0, "testDoInTransaction");
        } finally {
            tx.success();tx.finish();
        }
    }

    @Test
    public void shouldRollbackTransactionOnException() {
        try {
            neo4jTemplate.exec(new GraphCallback.WithoutResult() {
                @Override
                public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                    graph.getNodeById(node0.getId()).setProperty("test", "shouldRollbackTransactionOnException");
                    throw new RuntimeException("please rollback");
                }
            });
        } catch (RuntimeException re) {

        }
        Transaction tx=graphDatabase.beginTx();
        try {
            Assert.assertThat((String) node0.getProperty("test", "not set"), not("shouldRollbackTransactionOnException"));
        } finally {
            tx.success();tx.finish();
        }
    }

    @Test
    public void shouldRollbackViaStatus() throws Exception {
        new TransactionTemplate(neo4jTransactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                neo4jTemplate.exec(new GraphCallback.WithoutResult() {
                    @Override
                    public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                        node0.setProperty("test", "shouldRollbackTransactionOnException");
                        status.setRollbackOnly();
                    }
                });
            }
        });
        Transaction tx=graphDatabase.beginTx();
        try {
            Assert.assertThat((String) node0.getProperty("test", "not set"), not("shouldRollbackTransactionOnException"));
        } finally {
          tx.success();tx.finish();
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotConvertUserRuntimeExceptionToDataAccessException() {
        neo4jTemplate.exec(new GraphCallback.WithoutResult() {
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
                graph.createNode(null, null);
            }
        });
    }

    @Test(expected = DataAccessException.class)
    public void shouldConvertNotFoundExceptionToDataAccessException() {
        Neo4jTemplate template = new Neo4jTemplate(graphDatabase, neo4jTransactionManager);
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                graph.getNodeById(Long.MAX_VALUE);
            }
        });
    }

    @Test(expected = DataAccessException.class)
    public void shouldConvertTemplateNotFoundExceptionToDataAccessException() {
        neo4jTemplate.getNode(Long.MAX_VALUE);
    }

    @Test
    @Transactional
    public void shouldExecuteCallback() throws Exception {
        Long refNodeId = neo4jTemplate.exec(new GraphCallback<Long>() {
            @Override
            public Long doWithGraph(GraphDatabase graph) throws Exception {
                return graph.getNodeById(node0.getId()).getId();
            }
        });
        assertEquals(node0.getId(), (long) refNodeId);
    }

    @Test
    @Transactional
    public void testCreateNode() throws Exception {
        Node node = neo4jTemplate.createNode(null);
        assertNotNull("created node", node);
    }

    @Test
    @Transactional
    public void testCreateEntityWithProperties() throws Exception {
        Person person = neo4jTemplate.createNodeAs(Person.class, map("name", "name"));
        assertNotNull("created node", person);
        assertEquals("property created", "name", person.getName());
    }

    @Test
    @Transactional
    public void testCreateNodeTypeWithProperties() throws Exception {
        Node person = neo4jTemplate.createNodeAs(Node.class, map("name", "name"));
        assertNotNull("created node", person);
        assertEquals("property created", "name", person.getProperty("name"));
    }

    @Test
    @Transactional
    public void testCreateNodeWithProperties() throws Exception {
        Node node = neo4jTemplate.createNode(map("test", "testCreateNodeWithProperties"));
        assertTestPropertySet(node, "testCreateNodeWithProperties");
    }

    private void assertTestPropertySet(Node node, String testName) {
        assertEquals(testName, node.getProperty("test", "not set"));
    }

    @Test
    @Transactional
    public void testGetNode() throws Exception {
        Node lookedUpNode = neo4jTemplate.getNode(node0.getId());
        assertEquals(node0, lookedUpNode);
    }

    @Test
    @Transactional
    public void testGetRelationship() throws Exception {
        Relationship lookedUpRelationship = neo4jTemplate.getRelationship(relationship1.getId());
        assertThat(lookedUpRelationship, is(relationship1));

    }

    @Test
    @Transactional
    public void testIndexRelationship() throws Exception {
        Index<Relationship> index = graphDatabase.getIndex("relationship");
        Relationship lookedUpRelationship = index.get("name", "rel1").getSingle();
        assertThat("same relationship from index", lookedUpRelationship, is(relationship1));
    }

    @Test
    @Transactional
    public void testIndexNode() throws Exception {
        neo4jTemplate.index("node", node1, "name", "node1");
        Index<Node> index = graphDatabase.getIndex("node");
        Node lookedUpNode = index.get("name", "node1").getSingle();
        assertThat("same node from index", lookedUpNode, is(node1));
    }

    @Test
    @Transactional
    public void testQueryNodes() throws Exception {
        assertSingleResult("node0", neo4jTemplate.lookup("node", new TermQuery(new Term("name", "node0"))).to(String.class, new PropertyContainerNameConverter()));
    }

    @Test
    @Transactional
    public void testRetrieveNodes() throws Exception {
        assertSingleResult("node0", neo4jTemplate.lookup("node", "name", "node0").to(String.class, new PropertyContainerNameConverter()));
    }

    @Test
    @Transactional
    public void testQueryRelationships() throws Exception {
        assertSingleResult("rel1", neo4jTemplate.lookup("relationship", new TermQuery(new Term("name", "rel1"))).to(String.class, new PropertyContainerNameConverter()));
    }

    @Test
    @Transactional
    public void testRetrieveRelationships() throws Exception {
        assertSingleResult("rel1", neo4jTemplate.lookup("relationship", "name", "rel1").to(String.class, new PropertyContainerNameConverter()));
    }

    @SuppressWarnings("deprecation")
    @Test
    @Transactional
    public void testTraverse() throws Exception {
        //final TraversalDescription description = Traversal.description().relationships(KNOWS).prune(Traversal.pruneAfterDepth(1)).filter(Traversal.returnAllButStartNode());
        final TraversalDescription description = Traversal.description().relationships(KNOWS).evaluator(Evaluators.toDepth(1)).evaluator(Evaluators.excludeStartPosition());
        assertSingleResult("node1", neo4jTemplate.traverse(node0, description).to(String.class, new PathNodeNameMapper()));
    }

    @Test
    @Transactional
    public void shouldFindNextNodeViaCypher() throws Exception {
        assertSingleResult(node1, neo4jTemplate.query("start n=node(" + node0.getId() + ") match n-[:knows]->m return m", null).to(Node.class));
    }

    @Test
    @Transactional
    public void shouldGetDirectRelationship() throws Exception {
        assertSingleResult("rel1", neo4jTemplate.convert(node0.getRelationships(DynamicRelationshipType.withName("knows"))).to(String.class, new RelationshipNameConverter()));
    }

    @Test
    @Transactional
    public void shouldGetDirectRelationshipForType() throws Exception {
        assertSingleResult("rel1", neo4jTemplate.convert(node0.getRelationships(KNOWS)).to(String.class, new RelationshipNameConverter()));
    }

    @Test
    @Transactional
    public void shouldGetDirectRelationshipForTypeAndDirection() throws Exception {
        assertSingleResult("rel1", neo4jTemplate.convert(node0.getRelationships(KNOWS, Direction.OUTGOING)).to(String.class, new RelationshipNameConverter()));
    }

    private <T> void assertSingleResult(T expected, Iterable<T> iterable) {
        Iterator<T> result = iterable.iterator();
        assertEquals(expected, result.next());
        assertEquals(false, result.hasNext());
    }


    @Test
    @Transactional
    public void shouldCreateRelationshipWithProperty() throws Exception {
        Relationship relationship = neo4jTemplate.createRelationshipBetween(node0, node1, "has", map("name", "rel2"));
        assertNotNull(relationship);
        assertEquals(node0, relationship.getStartNode());
        assertEquals(node1, relationship.getEndNode());
        assertEquals(HAS.name(), relationship.getType().name());
        assertEquals("rel2", relationship.getProperty("name", "not set"));
    }

    private static class PathRelationshipNameMapper extends ResultConverter.ResultConverterAdapter<Path, String> {
        @Override
        public String convert(Path path, Class<String> type) {
            return (String) path.lastRelationship().getProperty("name", "not set");
        }
    }

    private static class PathNodeNameMapper extends ResultConverter.ResultConverterAdapter<Path, String> {
        @Override
        public String convert(Path path, Class<String> type) {
            return (String) path.endNode().getProperty("name", "not set");
        }
    }

    private static class RelationshipNameConverter extends ResultConverter.ResultConverterAdapter<Relationship, String> {
        @Override
        public String convert(Relationship value, Class<String> type) {
            return (String) value.getProperty("name");
        }
    }

    private static class PropertyContainerNameConverter extends ResultConverter.ResultConverterAdapter<PropertyContainer, String> {
        @Override
        public String convert(PropertyContainer value, Class<String> type) {
            return (String) value.getProperty("name");
        }
    }
}
