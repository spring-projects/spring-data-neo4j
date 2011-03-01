package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

public class TraversalTest {

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
    public void testTraverseFromGroupToPeople() {
        Person p = persistedPerson("Michael", 35);
        Group group = new Group().persist();
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = new TraversalDescriptionImpl().relationships(DynamicRelationshipType.withName("persons")).filter(Traversal.returnAllButStartNode());
        Iterable<Person> people = (Iterable<Person>) group.findAllByTraversal(Person.class, traversalDescription);
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        assertEquals(Collections.singleton(p),found);
    }

    @Test
    @Transactional
    @Rollback(false)
    public void testTraverseFieldFromGroupToPeople() {
        Person p = persistedPerson("Michael", 35);
        Group group = new Group().persist();
        group.setName("dev");
        group.addPerson(p);
        Iterable<Person> people = group.getPeople();
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        assertEquals(Collections.singleton(p),found);
    }

    @Test
    @Transactional
    public void testTraverseFromGroupToPeopleWithFinder() {
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Person p = persistedPerson("Michael", 35);
        Group group = new Group().persist();
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = new TraversalDescriptionImpl().relationships(DynamicRelationshipType.withName("persons")).filter(Traversal.returnAllButStartNode());
        Iterable<Person> people = finder.findAllByTraversal(group, traversalDescription);
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        assertEquals(Collections.singleton(p),found);
    }

}
