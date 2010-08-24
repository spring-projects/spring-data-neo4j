package org.springframework.persistence.graph.neo4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.persistence.test.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Michael Hunger
 * @since 20.08.2010
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/persistence/test/graph/Neo4jGraphPersistenceTest-context.xml"})
@Transactional
public class Neo4jEntityManagerTest {
    @Resource
    GraphDatabaseService graphDatabaseService;
    @PersistenceContext(unitName="neo4j-persistence")
    EntityManager entityManager;

    Person person;
	private Node node;

    @Before
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseService);
        node = graphDatabaseService.createNode();
        person = new Person(node);
    }

    @Test
    public void testPersist() throws Exception {
    	entityManager.persist(person);
    }

    @Test
    public void testMerge() throws Exception {
    	Person merged=entityManager.merge(person);
    	assertEquals(person,merged);
    }
    @Ignore
    @Test
    public void testRemove() throws Exception {
    	long nodeId=node.getId();
    	entityManager.remove(person);
    	final Node foundNode = graphDatabaseService.getNodeById(nodeId);
    	assertNull(foundNode);
    }
    @Test
    public void testFind() throws Exception {
    	final Person found = entityManager.find(Person.class, node.getId());
    	assertEquals(person,found);
    }

    @Test
    public void testGetReference() throws Exception {
    	final Person found = entityManager.getReference(Person.class, node.getId());
    	assertEquals(person,found);
    }

    @Test
    public void testFlush() throws Exception {
    }

    @Test
    public void testSetFlushMode() throws Exception {
    }

    @Test
    public void testGetFlushMode() throws Exception {
    }

    @Test
    public void testLock() throws Exception {
    }

    @Test
    public void testRefresh() throws Exception {
    	entityManager.refresh(person);
    	assertEquals(node, person.getUnderlyingNode());
    }
    @Test
    public void testClear() throws Exception {
    }

    @Test(expected=InvalidDataAccessApiUsageException.class)
    public void removeThrowsErrorForNonNodeBacked() throws Exception {
    	entityManager.remove(new Object());
    }
    
    @Test
    public void testContains() throws Exception {
    	final Person p2 = new Person("Rod",39);
        assertTrue(entityManager.contains(person));
        assertTrue(entityManager.contains(p2));
    	assertFalse(entityManager.contains(new Object()));
    }
/*
    @Test
    public void testCreateQuery() throws Exception {
    }

    @Test
    public void testCreateNamedQuery() throws Exception {
    }

    @Test
    public void testCreateNativeQuery() throws Exception {
    }

    @Test
    public void testJoinTransaction() throws Exception {
    }
*/
    @Test
    public void testGetDelegate() throws Exception {
    	assertTrue(entityManager.getDelegate() instanceof GraphDatabaseService);
    }

    @Test(expected=InvalidDataAccessApiUsageException.class)
    public void testClose() throws Exception {
    	entityManager.close();
    	assertFalse(entityManager.isOpen());
    	entityManager.refresh(person);
    }

    @Test
    public void testIsOpen() throws Exception {
    	assertTrue(entityManager.isOpen());
    }

    @Test
    public void testGetTransaction() throws Exception {
    	final EntityTransaction transaction = entityManager.getTransaction();
    	assertFalse(transaction.getRollbackOnly());
    	person.setName("Michael");
    	entityManager.persist(person);
    	transaction.setRollbackOnly();
    }
}
