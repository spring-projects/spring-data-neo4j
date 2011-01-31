package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Friendship;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class RelationshipEntityTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private FinderFactory finderFactory;

	@Before
	@Transactional
	public void cleanDb() {
		Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testRelationshipCreate() {
        Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        Relationship rel = p.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
        assertEquals(f.getUnderlyingState(), rel);
        assertEquals(p2.getUnderlyingState(), rel.getEndNode());
    }

    @Test
    @Transactional
    public void testRelationshipSetProperty() {
        Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.setYears(1);
        assertEquals(1, f.getUnderlyingState().getProperty("Friendship.years"));
    }

    @Test
    @Transactional
    public void testRelationshipGetProperty() {
        Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.getUnderlyingState().setProperty("Friendship.years", 1);
        assertEquals(1, f.getYears());
    }

    @Test
    @Transactional
    public void testRelationshipGetStartNodeAndEndNode() {
        Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(p, f.getPerson1());
        assertEquals(p2, f.getPerson2());
    }

    @Test
    @Transactional
    public void testGetRelationshipToReturnsRelationship() {
        Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(f,p.getRelationshipTo(p2,Friendship.class, "knows"));
    }
}
