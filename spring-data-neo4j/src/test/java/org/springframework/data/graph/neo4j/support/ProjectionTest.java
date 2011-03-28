package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Named;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

public class ProjectionTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private DirectGraphRepositoryFactory graphRepositoryFactory;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testProjectGroupToNamed() {
        Group group = new Group().persist();
        group.setName("developers");

        Named named = (Named)group.projectTo(Named.class);
        assertEquals("named.name","developers", named.getName());
        assertEquals("nameds node name property","developers", named.getPersistentState().getProperty("name"));
    }

}
