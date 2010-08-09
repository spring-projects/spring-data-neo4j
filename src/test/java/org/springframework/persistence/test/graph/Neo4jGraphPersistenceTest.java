package org.springframework.persistence.test.graph;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
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


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class Neo4jGraphPersistenceTest {

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
		int age = 39;
		Person p = new Person("Rod", age);
		Assert.assertEquals(p.getName(), p.getUnderlyingNode().getProperty("Person.name"));
		Assert.assertEquals(p.getAge(), p.getUnderlyingNode().getProperty("Person.age"));
		insertedId = p.getId();
	}
	
	@Test
	@Transactional
	public void testCreateRelationshipWithoutAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person spouse=new Person("Tina",36);
		p.setSpouse(spouse);
		Assert.assertEquals("Tina", p.getSpouse().getUnderlyingNode().getProperty("Person.name"));
		Node spouseNode=p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), org.neo4j.graphdb.Direction.OUTGOING).getEndNode();
		Assert.assertEquals(spouse.getUnderlyingNode(), spouseNode);
		Assert.assertEquals(spouse, p.getSpouse());
	}
	
	@Test
	@Ignore
	@Transactional
	public void testCreateRelationshipWithAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person mother=new Person("Gabi",60);
		p.setMother(mother);
		Assert.assertEquals("Gabi", p.getMother().getUnderlyingNode().getProperty("Person.name"));
		Node motherNode=p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("mother"), org.neo4j.graphdb.Direction.BOTH).getEndNode();
		Assert.assertEquals(mother.getUnderlyingNode(), motherNode);
		Assert.assertEquals(mother, p.getMother());
	}
	
	// TODO test delete relationship
	// TODO test delete previous relationship
	// TODO test incoming relationship
	// TODO test bidirectional relationship
	// TODO test remove property (set to null)
	
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
