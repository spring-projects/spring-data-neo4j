package org.springframework.datastore.graph.neo4j.jpa;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.*;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.neo4j.Person;
import org.springframework.datastore.graph.neo4j.spi.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Michael Hunger
 * @since 20.08.2010
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/datastore/graph/neo4j/spi/Neo4jGraphPersistenceTest-context.xml"})
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
        person = new Person("Michael",35);
        node = person.getUnderlyingNode();
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
    public void testFindSingle() throws Exception {
        final Query query = entityManager.createQuery("select o from Person o");
        final Person found = (Person) query.getSingleResult();
    	assertEquals(person,found);
    }
    @Test
    public void testFindAll() throws Exception {
        final Query query = entityManager.createQuery("select o from Person o");
        Collection<Person> people=query.getResultList();
    	Assert.assertEquals(asList(person),people);
    }

    @Test
    public void testFindAll2() throws Exception {
        final Person person2 = new Person("Rod", 39);
        final Query query = entityManager.createQuery("select o from Person o");
        Collection<Person> people=query.getResultList();
    	Assert.assertEquals(new HashSet<Person>(asList(person,person2)),new HashSet<Person>(people));
    }
    @Test
    public void testFindAllStart() throws Exception {
        final Query query = entityManager.createQuery("select o from Person o").setFirstResult(1);
        Collection<Person> people=query.getResultList();
    	Assert.assertEquals(Collections.<Person>emptySet(),new HashSet<Person>(people));
    }
    @Test
    public void testFindAllEnd() throws Exception {
        final Query query = entityManager.createQuery("select o from Person o").setMaxResults(0);
        Collection<Person> people=query.getResultList();
    	Assert.assertEquals(Collections.<Person>emptySet(),new HashSet<Person>(people));
    }
    @Test
    public void testFindAllStartEnd() throws Exception {
        new Person("Rod", 39);
        final Query query = entityManager.createQuery("select o from Person o").setMaxResults(1).setFirstResult(1);
        Collection<Person> people=query.getResultList();
    	Assert.assertEquals(1,people.size());
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

    @Ignore("Springs shared EM does not honor the contract of close")
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

    @Ignore("Springs shared EM does not allow getTransaction")
    @Test
    public void testGetTransaction() throws Exception {
    	final EntityTransaction transaction = entityManager.getTransaction();
    	assertFalse(transaction.getRollbackOnly());
    	person.setName("Michael");
    	entityManager.persist(person);
    	transaction.setRollbackOnly();
    }
}
