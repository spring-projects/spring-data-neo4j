package org.springframework.persistence.test.graph;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.persistence.graph.neo4j.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;
import org.springframework.persistence.test.Group;
import org.springframework.persistence.test.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class Neo4jGraphPersistenceTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private EntityInstantiator<NodeBacked,Node> nodeInstantiator;

	@Autowired
	protected GraphDatabaseService graphDatabaseService;
	
	private Long insertedId = 0L;
	
	@Test
	public void testStuffWasAutowired() {
        Assert.assertNotNull( graphDatabaseService );
        Assert.assertNotNull( nodeInstantiator );
	}

	@Before
	@Transactional
	public void testUserConstructor() {
		Person p = new Person("Rod", 39);
		Assert.assertEquals(p.getName(), p.getUnderlyingNode().getProperty("Person.name"));
		Assert.assertEquals(p.getAge(), p.getUnderlyingNode().getProperty("Person.age"));
		insertedId = p.getId();
	}
	
	@Test
	@Transactional
	public void testSetProperties() {
		Person p = new Person("Foo", 2);
		p.setName("Michael");
		p.setAge(35);
		Assert.assertEquals("Michael", p.getUnderlyingNode().getProperty("Person.name"));
		Assert.assertEquals(35, p.getUnderlyingNode().getProperty("Person.age"));
	}
	
	@Test
	@Transactional
	public void testCreateRelationshipWithoutAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina",36);
		p.setSpouse(spouse);
		Node spouseNode=p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING).getEndNode();
		Assert.assertEquals(spouse.getUnderlyingNode(), spouseNode);
		Assert.assertEquals(spouse, p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testCreateRelationshipWithAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person mother = new Person("Gabi",60);
		p.setMother(mother);
		Node motherNode = p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("mother"), Direction.OUTGOING).getEndNode();
		Assert.assertEquals(mother.getUnderlyingNode(), motherNode);
		Assert.assertEquals(mother, p.getMother());
	}

	@Test
	@Transactional
	public void testDeleteRelationship() {
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina", 36);
		p.setSpouse(spouse);
		p.setSpouse(null);
		Assert.assertNull(p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING));
		Assert.assertNull(p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testDeletePreviousRelationshipOnNewRelationship() {
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina", 36);
		Person friend = new Person("Helga", 34);
		p.setSpouse(spouse);
		p.setSpouse(friend);
		Assert.assertEquals(friend.getUnderlyingNode(), p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING).getEndNode());
		Assert.assertEquals(friend, p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testCreateIncomingRelationshipWithAnnotationOnSet() {
		Person p = new Person("David", 25);
		Person boss = new Person("Emil", 32);
		p.setBoss(boss);
		Assert.assertEquals(boss.getUnderlyingNode(), p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("boss"), Direction.INCOMING).getStartNode());
		Assert.assertEquals(boss, p.getBoss());
	}
	
	@Test(expected = InvalidDataAccessResourceUsageException.class)
	public void testCreateOutsideTransaction() {
		Person p = new Person("Michael", 35);
	}
	
	@Test(expected = InvalidDataAccessResourceUsageException.class)
	public void testSetPropertyOutsideTransaction() {
		Transaction tx = graphDatabaseService.beginTx();
		Person p = null;
		try {
			p = new Person("Michael", 35);
			tx.success();
		} finally {
			tx.finish();
		}
		p.setAge(25);
	}
	
	@Test(expected = InvalidDataAccessResourceUsageException.class)
	public void testCreateRelationshipOutsideTransaction() {
		Transaction tx = graphDatabaseService.beginTx();
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
	}
	
	@Test
	public void testGetPropertyOutsideTransaction() {
		Transaction tx = graphDatabaseService.beginTx();
		Person p = null;
		try {
			p = new Person("Michael", 35);
			tx.success();
		} finally {
			tx.finish();
		}
		Assert.assertEquals("Wrong age.", 35, p.getAge());
	}
	
	@Test(expected = InvalidDataAccessApiUsageException.class)
	@Transactional
	public void testCircularRelationship() {
		Person p = new Person("Michael", 35);
		p.setSpouse(p);
	}

	@Ignore
	@Test
	@Transactional
	public void testOneToManyRelationshipsWithoutAnnotation() {
		Person michael = new Person("Michael", 35);
		Person david = new Person("David", 25);
		Group group = new Group();
		Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
		group.setPersons(persons);
		Relationship michaelRel = michael.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Group.persons"), Direction.INCOMING);
		Relationship davidRel = david.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Group.persons"), Direction.INCOMING);
		Assert.assertEquals(group.getUnderlyingNode(), michaelRel.getStartNode());
		Assert.assertEquals(group.getUnderlyingNode(), davidRel.getStartNode());
		Assert.assertEquals(persons, group.getPersons());
	}

	@Test
	@Transactional
	public void testInstantiatedFinder() {
		Node n = findPersonTestNode();
		Person found = nodeInstantiator.createEntityFromState(n, Person.class);
		Assert.assertEquals("Rod", found.getUnderlyingNode().getProperty("Person.name"));
		Assert.assertEquals(39, found.getUnderlyingNode().getProperty("Person.age"));
	}

	@Test
	public void printNeo4jData() {
		StringBuilder ret = new StringBuilder();
		for (Node n : graphDatabaseService.getAllNodes()) {
			ret.append("ID: " + n.getId() + " [");
			int x = 0;
			for (String prop : n.getPropertyKeys()) {
				if (x++ > 0) {
					ret.append(", ");
				}
				ret.append(prop + "=" + n.getProperty(prop));				
			}
			ret.append("] ");			
		}
		System.out.println("*** NEO4J DATA: " + ret);
	}

	private Node findPersonTestNode() {
		return graphDatabaseService.getNodeById(insertedId);
	}

}
