package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.neo4j.*;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

public class PropertyTest {

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
    public void testSetPropertyEnum() {
        Person p = persistedPerson("Michael", 35);
        p.setPersonality(Personality.EXTROVERT);
        assertEquals("Wrong enum serialization.", "EXTROVERT", p.getPersistentState().getProperty("Person.personality"));
    }

    @Test
    @Transactional
    public void testGetPropertyEnum() {
        Person p = persistedPerson("Michael", 35);
        p.getPersistentState().setProperty("Person.personality", "EXTROVERT");
        assertEquals("Did not deserialize property value properly.", Personality.EXTROVERT, p.getPersonality());
    }

    @Test(expected = NotFoundException.class)
    @Transactional
    public void testSetTransientPropertyFieldNotManaged() {
        Person p = persistedPerson("Michael", 35);
        p.setThought("food");
        p.getPersistentState().getProperty("Person.thought");
    }

    @Test
    @Transactional
    public void testGetTransientPropertyFieldNotManaged() {
        Person p = persistedPerson("Michael", 35);
        p.setThought("food");
        p.getPersistentState().setProperty("Person.thought", "sleep");
        assertEquals("Should not have read transient value from graph.", "food", p.getThought());
    }
    @Test
    @Transactional
    @Rollback(false)
    public void testRelationshipSetPropertyDate() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setFirstMeetingDate(new Date(3));
        assertEquals("Date not serialized properly.", "3", f.getPersistentState().getProperty("Friendship.firstMeetingDate"));
    }

    @Test
    @Transactional
    public void testRelationshipGetPropertyDate() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.getPersistentState().setProperty("Friendship.firstMeetingDate", "3");
        assertEquals("Date not deserialized properly.", new Date(3), f.getFirstMeetingDate());
    }

    @Test(expected = NotFoundException.class)
    @Transactional
    public void testRelationshipSetTransientPropertyFieldNotManaged() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setLatestLocation("Menlo Park");
        f.getPersistentState().getProperty("Friendship.latestLocation");
    }

    @Test
    @Transactional
    public void testRelationshipGetTransientPropertyFieldNotManaged() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setLatestLocation("Menlo Park");
        f.getPersistentState().setProperty("Friendship.latestLocation", "Palo Alto");
        assertEquals("Should not have read transient value from graph.", "Menlo Park", f.getLatestLocation());
    }

    @Test
    @Transactional
    public void testEntityIdField() {
        Person p = persistedPerson("Michael", 35);
        assertEquals("Wrong ID.", p.getPersistentState().getId(), p.getId());
    }

    @Test
    @Transactional
    public void testRelationshipIdField() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
		assertEquals("Wrong ID.", (Long)f.getPersistentState().getId(), f.getRelationshipId());
    }

	@Test(expected = InvalidDataAccessApiUsageException.class)
	@Transactional
	public void testFailFastOnMisconfiguredOneToNProperty() {
	    new InvalidOneToNEntity();
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	@Transactional
	public void testFailFastOnMisconfiguredReadOnlyOneToNProperty() {
	    new InvalidReadOnlyOneToNEntity();
	}
}
