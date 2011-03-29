package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.repository.NodeGraphRepository;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

    public class NodeEntityTest {

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
    public void testUserConstructor() {
        Person p = persistedPerson("Rod", 39);
        assertEquals(p.getName(), p.getPersistentState().getProperty("Person.name"));
        assertEquals(p.getAge(), p.getPersistentState().getProperty("Person.age"));
        Person found = graphDatabaseContext.createEntityFromState(graphDatabaseContext.getNodeById(p.getNodeId()), Person.class);
        assertEquals("Rod", found.getPersistentState().getProperty("Person.name"));
        assertEquals(39, found.getPersistentState().getProperty("Person.age"));
    }

    @Test
    @Transactional
    public void testSetProperties() {
        String name = "Michael";
        int age = 35;
        short height = 182;

        Person p = persistedPerson("Foo", 2);
        p.setName( name );
        p.setAge( age );
        p.setHeight( height );
        assertEquals( name, p.getPersistentState().getProperty( "Person.name" ) );
        assertEquals( age, p.getPersistentState().getProperty("Person.age"));
        assertEquals((Short)height, p.getHeight());
    }

    @Test
    @Transactional
    public void testSetShortProperty() {
        Person p = persistedPerson("Foo", 2);
        p.setHeight((short)182);
        assertEquals((Short)(short)182, p.getHeight());
        assertEquals((short)182, p.getPersistentState().getProperty("Person.height"));
    }
    @Test
    @Transactional
    public void testSetShortNameProperty() {
        Group group = new Group().persist();
        group.setName("developers");
        assertEquals("developers", group.getPersistentState().getProperty("name"));
    }
    // own transaction handling because of http://wiki.neo4j.org/content/Delete_Semantics
    @Test(expected = NotFoundException.class)
    public void testDeleteEntityFromGDC() {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        graphDatabaseContext.removeNodeEntity(spouse);
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        NodeGraphRepository<Person> finder = graphRepositoryFactory.createNodeEntityRepository(Person.class);
        Person spouseFromIndex = finder.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index",spouseFromIndex);
        Assert.assertNull("node deleted " + id, graphDatabaseContext.getNodeById(id));
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteEntity() {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        spouse.remove();
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        NodeGraphRepository<Person> finder = graphRepositoryFactory.createNodeEntityRepository(Person.class);
        Person spouseFromIndex = finder.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index", spouseFromIndex);
        Assert.assertNull("node deleted " + id, graphDatabaseContext.getNodeById(id));
    }

}
