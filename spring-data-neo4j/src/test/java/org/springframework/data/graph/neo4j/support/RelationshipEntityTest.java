package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Friendship;
import org.springframework.data.graph.neo4j.Person;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
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

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testRelationshipCreate() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        Relationship rel = p.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
        assertEquals(f.getPersistentState(), rel);
        assertEquals(p2.getPersistentState(), rel.getEndNode());
    }

    @Test
    @Transactional
    public void testRelationshipSetProperty() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setYears(1);
        assertEquals(1, f.getPersistentState().getProperty("Friendship.years"));
    }

    @Test
    @Transactional
    public void testRelationshipGetProperty() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.getPersistentState().setProperty("Friendship.years", 1);
        assertEquals(1, f.getYears());
    }

    @Test
    @Transactional
    public void testRelationshipGetStartNodeAndEndNode() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(p, f.getPerson1());
        assertEquals(p2, f.getPerson2());
    }

    @Test
    @Transactional
    public void testGetRelationshipToReturnsRelationship() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(f,p.getRelationshipTo(p2,Friendship.class, "knows"));
    }
}
