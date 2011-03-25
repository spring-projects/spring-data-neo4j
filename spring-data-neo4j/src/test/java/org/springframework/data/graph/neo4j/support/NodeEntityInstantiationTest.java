package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml",
        "classpath:org/springframework/data/graph/neo4j/support/PersonDirectCreator-context.xml" })
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})

    public class NodeEntityInstantiationTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private FinderFactory finderFactory;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testCreatePersonWithCreator() {
        Person p = persistedPerson("Rod", 39);
        long nodeId = p.getNodeId();

        Node node = graphDatabaseContext.getNodeById(nodeId);
        Person person1 = (Person) graphDatabaseContext.createEntityFromStoredType(node);
        assertEquals("Rod", person1.getName());
        Person person2 = graphDatabaseContext.createEntityFromState(node,Person.class);
        assertEquals("Rod", person2.getName());

        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Person found = finder.findById(nodeId);
        assertEquals("Rod", found.getName());
    }
}
