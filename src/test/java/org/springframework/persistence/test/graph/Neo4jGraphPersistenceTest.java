package org.springframework.persistence.test.graph;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.persistence.graph.Direction;
import org.springframework.persistence.graph.neo4j.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;
import org.springframework.persistence.test.Person;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class Neo4jGraphPersistenceTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private EntityInstantiator<NodeBacked,Node> nodeInstantiator;

	@Autowired
	protected GraphDatabaseService graphDatabaseService;
	
	private static Long insertedId = 0L;
	
	@Test
	public void testStuffWasAutowired() {
        Assert.assertNotNull( graphDatabaseService );
        Assert.assertNotNull( nodeInstantiator );
	}

	@Test
	@Transactional
	@Rollback(false)
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
		log.debug("testCreateRelationshipWithoutAnnotationOnSet");
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina",36);
		p.setSpouse(spouse);
		Node spouseNode=p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), org.neo4j.graphdb.Direction.OUTGOING).getEndNode();
		Assert.assertEquals(spouse.getUnderlyingNode(), spouseNode);
		Assert.assertEquals(spouse, p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testCreateRelationshipWithAnnotationOnSet() {
		log.debug("testCreateRelationshipWithAnnotationOnSet");
		Person p = new Person("Michael", 35);
		Person mother = new Person("Gabi",60);
		p.setMother(mother);
		Node motherNode = p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("mother"), org.neo4j.graphdb.Direction.OUTGOING).getEndNode();
		Assert.assertEquals(mother.getUnderlyingNode(), motherNode);
		Assert.assertEquals(mother, p.getMother());
	}

	// TODO test delete relationship
	@Test
	@Transactional
	public void testDeleteRelationship() {
		log.debug("testDeleteRelationship");
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina", 36);
		p.setSpouse(spouse);
		p.setSpouse(null);
		Assert.assertNull(p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), org.neo4j.graphdb.Direction.OUTGOING));
		Assert.assertNull(p.getSpouse());
	}
	
	// TODO test delete previous relationship
	@Test
	@Transactional
	public void testDeletePreviousRelationshipOnNewRelationship() {
		log.debug("testDeletePreviousRelationshipOnNewRelationship");
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina", 36);
		Person friend = new Person("Helga", 34);
		p.setSpouse(spouse);
		p.setSpouse(friend);
		Assert.assertEquals(friend.getUnderlyingNode(), p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), org.neo4j.graphdb.Direction.OUTGOING).getEndNode());
		Assert.assertEquals(friend, p.getSpouse());
	}
	
	// TODO test incoming relationship
	@Test
	@Transactional
	public void testCreateIncomingRelationshipWithAnnotationOnSet() {
		Person p = new Person("David", 25);
		Person boss = new Person("Emil", 32);
		p.setBoss(boss);
		Assert.assertEquals(boss.getUnderlyingNode(), p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("boss"), org.neo4j.graphdb.Direction.INCOMING).getStartNode());
		Assert.assertEquals(boss, p.getBoss());
	}
	
	// TODO test bidirectional relationship
	@Ignore
	@Test
	@Transactional
	public void testBidirectionalRelationshipWithAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person friend = new Person("David", 25);
		p.setFriend(friend);
	}
	
	// TODO test remove property (set to null)
	@Test
	@Transactional
	public void testInstantiatedFinder() {
		log.debug("testInstantiatedFinder");
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
