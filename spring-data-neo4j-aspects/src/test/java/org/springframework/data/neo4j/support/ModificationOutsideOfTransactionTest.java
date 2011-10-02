/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.Friendship;
import org.springframework.data.neo4j.*;
import org.springframework.data.neo4j.Group;
import org.springframework.data.neo4j.Person;
import org.springframework.data.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.springframework.data.neo4j.Person.persistedPerson;
import static org.springframework.data.neo4j.support.HasRelationshipMatcher.hasNoRelationship;
import static org.springframework.data.neo4j.support.HasRelationshipMatcher.hasRelationship;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:org/springframework/data/neo4j/support/Neo4jGraphPersistenceTest-context.xml"} )
public class ModificationOutsideOfTransactionTest
{

    protected final Log log = LogFactory.getLog( getClass() );

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Autowired
    private DirectGraphRepositoryFactory graphRepositoryFactory;

    @Before
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
	public void testCreateOutsideTransaction() {
		Person p = new Person("Michael", 35);
		assertEquals(35, p.getAge());
		p.setAge(36);
		assertEquals(36, p.getAge());
		assertFalse(hasPersistentState(p));
		p.persist();
		assertEquals(36, nodeFor(p).getProperty("age"));
	}

	@Test
	public void testCreateSubgraphOutsideOfTransactionPersistInDirectionOfRel() {
		Person michael = new Person("Michael", 35);
		Person emil = new Person("Emil", 31);

		michael.setBoss(emil);

		assertEquals(emil, michael.getBoss());
		assertFalse(hasPersistentState(michael));
		assertFalse(hasPersistentState(emil));
		michael.persist();
		assertThat(nodeFor(michael), hasRelationship("boss", nodeFor(emil)));
		assertThat(nodeFor(emil), hasRelationship("boss", nodeFor(michael)));

	}

	@Test
	public void testCreateSubgraphOutsideOfTransactionPersistWithImmediateCycle() {
		Person michael = new Person("Michael", 35);
		Person emil = new Person("Emil", 31);

		michael.setBoss(emil);
		emil.setBoss(michael);

		assertEquals(emil, michael.getBoss());
		assertEquals(michael, emil.getBoss());
		assertFalse(hasPersistentState(michael));
		assertFalse(hasPersistentState(emil));
		michael.persist();
		assertThat(nodeFor(michael), hasRelationship("boss", nodeFor(emil)));
		assertThat(nodeFor(emil), hasRelationship("boss", nodeFor(michael)));
	}

	@Test
	public void testCreateSubgraphOutsideOfTransactionPersistWithCycle() {
		Person michael = new Person("Michael", 35);
		Person david = new Person("David", 27);
		Person emil = new Person("Emil", 31);

		michael.setBoss(emil);
		david.setBoss(michael);
		emil.setBoss(david);

		assertEquals(emil, michael.getBoss());
		assertEquals(michael, david.getBoss());
		assertEquals(david, emil.getBoss());
		assertFalse(hasPersistentState(michael));
		assertFalse(hasPersistentState(david));
		assertFalse(hasPersistentState(emil));
		michael.persist();
		assertThat(nodeFor(michael), hasRelationship("boss", nodeFor(emil)));
		assertThat(nodeFor(michael), hasRelationship("boss", nodeFor(david)));
		assertThat(nodeFor(david), hasRelationship("boss", nodeFor(michael)));
		assertThat(nodeFor(david), hasRelationship("boss", nodeFor(emil)));
		assertThat(nodeFor(emil), hasRelationship("boss", nodeFor(david)));
		assertThat(nodeFor(emil), hasRelationship("boss", nodeFor(michael)));
	}

	@Ignore("ignored until subgraph persisting is added")
	@Test
	public void testCreateSubgraphOutsideOfTransactionPersistInReverseDirectionOfRel() {
		Person michael = new Person("Michael", 35);
		Person emil = new Person("Emil", 31);

		michael.setBoss(emil);

		assertEquals(emil, michael.getBoss());
		assertFalse(hasPersistentState(michael));
		assertFalse(hasPersistentState(emil));
		emil.persist();
		assertThat(nodeFor(michael), hasRelationship("boss", nodeFor(emil)));
		assertThat(nodeFor(emil), hasRelationship("boss", nodeFor(michael)));
	}

    // TODO: Would be nice if this worked outside of a tx
    @Test(expected = NotInTransactionException.class)
    public void foo() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 26);
        Friendship f = p.knows(p2);
    }

	@Test
    public void testSetPropertyOutsideTransaction()
    {
        Person p = persistedPerson( "Michael", 35 );
        p.setAge( 25 );
        assertEquals(25, p.getAge());
        assertEquals( 35, nodeFor( p ).getProperty("age") );
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
        assertThat( nodeFor( p ), hasNoRelationship( "spouse",spouse.getPersistentState() ) );


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
        assertThat( nodeFor( p ), hasRelationship( "spouse" ) );


        Person spouse2 = persistedPerson( "Rana", 5 );
        p.setSpouse( spouse2 );
        assertEquals( spouse2, p.getSpouse() );
    }

    private boolean hasPersistentState( Person person )
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
        final GraphRepository<Person> finder = graphRepositoryFactory.createGraphRepository(Person.class);
        assertEquals( false, finder.findAll().iterator().hasNext() );
    }

}
