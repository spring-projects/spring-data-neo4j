package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

public class FinderTest {

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
    public void testFinderFindAll() {
        Person p1 = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Iterable<Person> allPersons = finder.findAll();
        assertEquals(new HashSet<Person>(Arrays.asList(p1, p2)), IteratorUtil.addToCollection(allPersons.iterator(), new HashSet<Person>()));
    }

    @Test
    @Transactional
    public void testFinderFindById() {
        Person p = new Person("Michael", 35);
        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Person pById = finder.findById(p.getNodeId());
        assertEquals(p, pById);
    }

    @Test
    @Transactional
    public void testFinderFindByIdNonexistent() {
        Person p = new Person("Michael", 35);
        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Person p2 = finder.findById(589736218);
        Assert.assertNull(p2);
    }

    @Test
    @Transactional
    public void testFinderCount() {
        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        assertEquals(0, finder.count());
        Person p = new Person("Michael", 35);
        assertEquals(1, finder.count());
    }
    @Test
	@Transactional
	public void testFindAllOnGroup() {
	    log.debug("FindAllOnGroup start");
        Group g=new Group();
        g.setName("test");
        Group g2=new Group();
        g.setName("test");
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        Collection<Group> groups = IteratorUtil.addToCollection(finder.findAll().iterator(), new HashSet<Group>());
        Assert.assertEquals(2, groups.size());
	    log.debug("FindAllOnGroup done");
	}
}
