package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.neo4j.*;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.finder.RelationshipFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
    public class NodeEntityTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private FinderFactory finderFactory;

	@Before
	public void cleanDb() {
		Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testUserConstructor() {
        Person p = new Person("Rod", 39);
        assertEquals(p.getName(), p.getUnderlyingState().getProperty("Person.name"));
        assertEquals(p.getAge(), p.getUnderlyingState().getProperty("Person.age"));
        Person found = graphDatabaseContext.createEntityFromState(graphDatabaseContext.getNodeById(p.getNodeId()), Person.class);
        assertEquals("Rod", found.getUnderlyingState().getProperty("Person.name"));
        assertEquals(39, found.getUnderlyingState().getProperty("Person.age"));
    }

    @Test
    @Transactional
    public void testSetProperties() {
        Person p = new Person("Foo", 2);
        p.setName("Michael");
        p.setAge(35);
        p.setHeight((short)182);
        assertEquals("Michael", p.getUnderlyingState().getProperty("Person.name"));
        assertEquals(35, p.getUnderlyingState().getProperty("Person.age"));
        assertEquals((short)182, p.getUnderlyingState().getProperty("Person.height"));
        assertEquals((short)182, (short)p.getHeight());
    }
    @Test
    @Transactional
    public void testSetShortProperty() {
        Group group = new Group();
        group.setName("developers");
        assertEquals("developers", group.getUnderlyingState().getProperty("name"));
    }
    // own transaction handling because of http://wiki.neo4j.org/content/Delete_Semantics
    @Test(expected = NotFoundException.class)
    public void testDeleteEntityFromGDC() {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = new Person("Michael", 35);
        Person spouse = new Person("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        graphDatabaseContext.removeNodeEntity(spouse);
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Person spouseFromIndex = finder.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index",spouseFromIndex);
        Assert.assertNull("node deleted " + id, graphDatabaseContext.getNodeById(id));
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteEntity() {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = new Person("Michael", 35);
        Person spouse = new Person("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        spouse.remove();
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Person spouseFromIndex = finder.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index", spouseFromIndex);
        Assert.assertNull("node deleted " + id, graphDatabaseContext.getNodeById(id));
    }

}
