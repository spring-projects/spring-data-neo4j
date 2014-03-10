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
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Iterator;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;


public class Neo4jTemplateApiTests {
    private static final DynamicRelationshipType KNOWS = DynamicRelationshipType.withName("knows");
    private static final DynamicRelationshipType HAS = DynamicRelationshipType.withName("has");
    protected Neo4jTemplate template;
    protected GraphDatabase graphDatabase;
    protected Node node0;
    protected Relationship relationship1;
    protected Node node1;
    protected PlatformTransactionManager transactionManager;
    protected GraphDatabaseService graphDatabaseService;
    private Transaction transaction;


    @Before
    public void setUp() throws Exception
    {
        graphDatabaseService = createGraphDatabaseService();
        graphDatabase = createGraphDatabase();
        transactionManager = createTransactionManager();
        template = new Neo4jTemplate(graphDatabase, transactionManager);
        createData();
        transaction = graphDatabase.beginTx();
    }

    protected GraphDatabaseService createGraphDatabaseService() throws IOException {
        return new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    protected GraphDatabase createGraphDatabase() throws Exception {
        return new DelegatingGraphDatabase(graphDatabaseService);
    }

    protected PlatformTransactionManager createTransactionManager() {
        return new JtaTransactionManager(new SpringTransactionManager((GraphDatabaseAPI)graphDatabaseService));
    }

    private void createData() {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
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

    @After
    public void tearDown() throws Exception {
        if (transaction!=null) {
            transaction.success();
            transaction.finish();
        }
        if (graphDatabaseService!=null) {
            graphDatabaseService.shutdown();
        }
    }

    @Test(expected = DataAccessException.class)
    public void shouldConvertTemplateNotFoundExceptionToDataAccessException() {
        template.getNode(Long.MAX_VALUE);
    }

    @Test
    public void testGetNode() throws Exception {
        Node lookedUpNode = template.getNode(node0.getId());
        assertEquals(node0,lookedUpNode);
    }

    @Test
    public void testGetRelationship() throws Exception {
        Relationship lookedUpRelationship = template.getRelationship(relationship1.getId());
        assertThat(lookedUpRelationship, is(relationship1));

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
        assertThat("same node from index", lookedUpNode, is(node1));
    }

    @Test
    public void testQueryNodes() throws Exception {
        assertSingleResult("node0", template.lookup("node", new TermQuery(new Term("name", "node0"))).to(String.class, new PropertyContainerNameConverter()));
    }

    @Test
    public void testRetrieveNodes() throws Exception {
        assertSingleResult("node0", template.lookup("node", "name", "node0").to(String.class, new PropertyContainerNameConverter()));
    }

    @Test
    public void testQueryRelationships() throws Exception {
        assertSingleResult("rel1", template.lookup("relationship", new TermQuery(new Term("name", "rel1"))).to(String.class, new PropertyContainerNameConverter()));
    }

    @Test
    public void testRetrieveRelationships() throws Exception {
        assertSingleResult("rel1",template.lookup("relationship", "name", "rel1").to(String.class, new PropertyContainerNameConverter()));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testTraverse() throws Exception {
        //final TraversalDescription description = Traversal.description().relationships(KNOWS).prune(Traversal.pruneAfterDepth(1)).filter(Traversal.returnAllButStartNode());
        final TraversalDescription description = Traversal.description().relationships(KNOWS).evaluator(Evaluators.toDepth(1)).evaluator(Evaluators.excludeStartPosition());
        assertSingleResult("node1",template.traverse(node0, description).to(String.class,new PathNodeNameMapper()));
    }

    @Test
    public void shouldFindNextNodeViaCypher() throws Exception {
        assertSingleResult(node1, template.query("start n=node(" + node0.getId() + ") match n-->m return m", null).to(Node.class));
    }

    @Test
    public void shouldGetDirectRelationship() throws Exception {
        assertSingleResult("rel1", template.convert(node0.getRelationships()).to(String.class, new RelationshipNameConverter()));
    }
    @Test
    public void shouldGetDirectRelationshipForType() throws Exception {
        assertSingleResult("rel1", template.convert(node0.getRelationships(KNOWS)).to(String.class, new RelationshipNameConverter()));
    }
    @Test
    public void shouldGetDirectRelationshipForTypeAndDirection() throws Exception {
        assertSingleResult("rel1", template.convert(node0.getRelationships(KNOWS, Direction.OUTGOING)).to(String.class, new RelationshipNameConverter()));
    }

    private <T> void assertSingleResult(T expected, Iterable<T> iterable) {
        Iterator<T> result = iterable.iterator();
        assertEquals(expected, result.next());
        assertEquals(false, result.hasNext());
    }


    @Test
    public void shouldCreateRelationshipWithProperty() throws Exception {
        Relationship relationship = template.createRelationshipBetween(node0, node1, "has", map("name", "rel2"));
        assertNotNull(relationship);
        assertEquals(node0, relationship.getStartNode());
        assertEquals(node1,relationship.getEndNode());
        assertEquals(HAS.name(), relationship.getType().name());
        assertEquals("rel2",relationship.getProperty("name", "not set"));
    }

    private static class PathNodeNameMapper extends ResultConverter.ResultConverterAdapter<Path,String> {
        @Override
        public String convert(Path path, Class<String> type) {
            return (String) path.endNode().getProperty("name","not set");
        }
    }

    private static class RelationshipNameConverter  extends ResultConverter.ResultConverterAdapter<Relationship,String> {
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
