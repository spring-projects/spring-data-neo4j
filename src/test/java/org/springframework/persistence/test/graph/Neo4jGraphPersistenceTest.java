package org.springframework.persistence.test.graph;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
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
