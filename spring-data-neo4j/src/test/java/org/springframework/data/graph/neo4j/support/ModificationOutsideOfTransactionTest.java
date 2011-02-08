package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class ModificationOutsideOfTransactionTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private FinderFactory finderFactory;

	@Before
	@Transactional
	public void cleanDb() {
        Transaction tx = graphDatabaseContext.beginTx();
        try {
            Neo4jHelper.cleanDb(graphDatabaseContext);
            tx.success();
        } finally {
            tx.finish();
        }
    }

	@Test
    public void testCreateOutsideTransaction() {
		Person p = new Person("Michael", 35);
        assertEquals(35, p.getAge());
        assertFalse(hasUnderlyingNode(p));
	}

    @Test
	public void testCreateSubgraphOutsideOfTransaction() {
		Person michael = new Person("Michael", 35);
		Person emil = new Person("Emil", 35);
        michael.setBoss(emil);
        assertEquals(emil, michael.getBoss());
        assertTrue(hasUnderlyingNode(michael));
        assertNotNull(nodeFor(michael).getSingleRelationship(DynamicRelationshipType.withName("Person.boss"), Direction.INCOMING));
	}

    private boolean hasUnderlyingNode(Person person) {
        return person.hasUnderlyingNode();
    }

    private Node nodeFor(Person person) {
        return person.getUnderlyingState();
    }

    @Test
	public void testSetPropertyOutsideTransaction() {
		Transaction tx = graphDatabaseContext.beginTx();
		Person p = null;
		try {
			p = new Person("Michael", 35);
			tx.success();
		} finally {
			tx.finish();
		}
		p.setAge(25);
        assertEquals(25,p.getAge());
        tx = graphDatabaseContext.beginTx();
        try {
            assertEquals(25,p.getAge());
            assertEquals(25, nodeFor(p).getProperty("Person.age"));
            p.setAge(20);
            tx.success();
        } finally {
            tx.finish();
        }
        assertEquals(20,p.getAge());
	}

    @Test
	public void testCreateRelationshipOutsideTransaction() {
		Transaction tx = graphDatabaseContext.beginTx();
		Person p = null;
		Person spouse = null;
		try {
			p = new Person("Michael", 35);
			spouse = new Person("Tina", 36);
			tx.success();
		} finally {
			tx.finish();
		}
		p.setSpouse(spouse);
        assertEquals(spouse,p.getSpouse());
        Person spouse2;
        tx = graphDatabaseContext.beginTx();
        try {
            assertEquals(spouse,p.getSpouse());
            assertNotNull(nodeFor(p).getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING));
            spouse2 = new Person("Rana", 5);
            p.setSpouse(spouse2);
            tx.success();
        } finally {
            tx.finish();
        }
        assertEquals(spouse2,p.getSpouse());
	}

	@Test
	public void testGetPropertyOutsideTransaction() {
		Transaction tx = graphDatabaseContext.beginTx();
		Person p = null;
		try {
			p = new Person("Michael", 35);
			tx.success();
		} finally {
			tx.finish();
		}
		assertEquals("Wrong age.", (int)35, (int)p.getAge());
	}

    @Test
	public void testFindOutsideTransaction() {
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        assertEquals(false,finder.findAll().iterator().hasNext());
	}

}
