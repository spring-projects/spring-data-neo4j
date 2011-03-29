package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;
import static org.springframework.data.graph.neo4j.support.HasRelationshipMatcher.hasNoRelationship;
import static org.springframework.data.graph.neo4j.support.HasRelationshipMatcher.hasRelationship;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"} )

public class ModificationOutsideOfTransactionTest
{

    protected final Log log = LogFactory.getLog( getClass() );

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Autowired
    private FinderFactory finderFactory;

    @Before
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

	public void testCreateOutsideTransaction() {
		Person p = new Person("Michael", 35);
		assertEquals(35, p.getAge());
		p.setAge(36);
		assertEquals(36, p.getAge());
		assertFalse(hasUnderlyingNode(p));
		p.persist();
		assertEquals(36, nodeFor(p).getProperty("Person.age"));
	}

	@Test
    @Ignore("ignored until subgraph persisting is added")
	public void testCreateSubgraphOutsideOfTransactionPersistInDirectionOfRel() {
		Person michael = new Person("Michael", 35);
		Person emil = new Person("Emil", 31);

		michael.setBoss(emil);

		assertEquals(emil, michael.getBoss());
		assertFalse(hasUnderlyingNode(michael));
		assertFalse(hasUnderlyingNode(emil));
		michael.persist();
		assertThat(nodeFor(michael), hasRelationship("boss", nodeFor(emil)));
		assertThat(nodeFor(emil), hasRelationship("boss", nodeFor(michael)));

	}

	@Ignore("ignored until subgraph persisting is added")
	@Test
	public void testCreateSubgraphOutsideOfTransactionPersistInReverseDirectionOfRel() {
		Person michael = new Person("Michael", 35);
		Person emil = new Person("Emil", 31);

		michael.setBoss(emil);

		assertEquals(emil, michael.getBoss());
		assertFalse(hasUnderlyingNode(michael));
		assertFalse(hasUnderlyingNode(emil));
		emil.persist();
		assertThat(nodeFor(michael), hasRelationship("boss", nodeFor(emil)));
		assertThat(nodeFor(emil), hasRelationship("boss", nodeFor(michael)));
	}

	@Test
    public void testSetPropertyOutsideTransaction()
    {
        Person p = persistedPerson( "Michael", 35 );
        p.setAge( 25 );
        assertEquals(25, p.getAge());
        assertEquals( 35, nodeFor( p ).getProperty("Person.age") );
    }

	@Ignore
	@Test
    public void shouldWorkWithUninitializedCollectionFieldWithoutUnderlyingState()
    {
        Group group = new Group();
	    Collection<Person> people = group.getPersons();
	    assertNotNull(people);

	    Person p = new Person( "David", 27 );
	    people.add(p);

        assertEquals( Collections.singleton(p), group.getPersons() );
    }

	@Test
    public void shouldWorkWithInitializedCollectionFieldWithoutUnderlyingState()
    {
        Group group = new Group();
	    group.setPersons(new HashSet<Person>());
	    Collection<Person> people = group.getPersons();
	    assertNotNull(people);

	    Person p = new Person( "David", 27 );
	    people.add(p);

        assertEquals( Collections.singleton(p), group.getPersons() );
    }

    @Test
    public void shouldNotCreateGraphRelationshipOutsideTransaction()
    {
        Person p = persistedPerson( "Michael", 35 );
        Person spouse = persistedPerson( "Tina", 36 );

        p.setSpouse( spouse );

        assertEquals( spouse, p.getSpouse() );
        assertThat( nodeFor( p ), hasNoRelationship( "Person.spouse",spouse.getPersistentState() ) );


        Person spouse2 = persistedPerson( "Rana", 5 );
        p.setSpouse( spouse2 );
        assertEquals( spouse2, p.getSpouse() );
    }

    @Test
    public void testCreateRelationshipOutsideTransactionAndPersist()
    {
        Person p = persistedPerson( "Michael", 35 );
        Person spouse = persistedPerson( "Tina", 36 );

        p.setSpouse( spouse );
        p.persist();

        assertEquals( spouse, p.getSpouse() );
        assertThat( nodeFor( p ), hasRelationship( "Person.spouse" ) );


        Person spouse2 = persistedPerson( "Rana", 5 );
        p.setSpouse( spouse2 );
        assertEquals( spouse2, p.getSpouse() );
    }

    private boolean hasUnderlyingNode( Person person )
    {
        return person.hasPersistentState();
    }

    private Node nodeFor( Person person )
    {
        return person.getPersistentState();
    }

    @Test
    public void testGetPropertyOutsideTransaction()
    {
        Person p = persistedPerson( "Michael", 35 );
        assertEquals( "Wrong age.", 35, p.getAge() );
    }

    @Test
    public void testFindOutsideTransaction()
    {
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder( Person.class );
        assertEquals( false, finder.findAll().iterator().hasNext() );
    }

}
