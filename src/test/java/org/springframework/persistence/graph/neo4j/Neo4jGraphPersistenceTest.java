package org.springframework.persistence.graph.neo4j;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class Neo4jGraphPersistenceTest {

	@Autowired
	protected GraphDatabaseService graphDatabaseService;
	
	@Test
	public void testGraphDatabaseServiceCreatedAndAutowired() {
        Assert.assertNotNull( graphDatabaseService );
	}
    
}
