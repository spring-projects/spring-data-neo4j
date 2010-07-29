package org.springframework.persistence.test.graph;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.persistence.test.Person;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class Neo4jGraphPersistenceTest {

	@Autowired
	protected GraphDatabaseService graphDatabaseService;
	
	@Test
	public void testGraphDatabaseServiceCreatedAndAutowired() {
        Assert.assertNotNull( graphDatabaseService );
	}

	@Test
	@Transactional
	@Rollback(false)
	public void testUserConstructor() {
		int age = 39;
		Person p = new Person("Rod", age);
		Assert.assertEquals(p.getUnderlyingNode().getProperty("Person.name"), p.getName());
		Assert.assertEquals(age, p.getAge());
	}

}
