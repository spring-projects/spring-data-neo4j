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

package org.springframework.data.neo4j.aspects.support;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;
import static org.springframework.data.neo4j.aspects.support.HasRelationshipMatcher.hasNoRelationship;
import static org.springframework.data.neo4j.aspects.support.HasRelationshipMatcher.hasRelationship;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"} )
public class ModificationOutsideOfTransactionTests extends EntityTestBase {

    @Before
    public void cleanUp() {
        manualCleanDb();
    }

    @Test
	public void testCreateOutsideTransaction() {
		Person p = new Person("Michael", 35);
		assertEquals(35, p.getAge());
		p.setAge(36);
		assertEquals(36, p.getAge());
		assertFalse(hasPersistentState(p));
        persist(p);
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
        persist(michael);
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
        persist(michael);
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
        persist(michael);
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
        persist(emil);
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

    @Test
    public void shouldWorkWithUninitializedCollectionFieldWithoutUnderlyingState() {
        Group group = new Group();
        Collection<Person> people = group.getPersons();
        assertNotNull(people);

        Person p = new Person("David", 27);
        people.add(p);

        assertEquals(Collections.singleton(p), group.getPersons());

        persist(group);
        assertThat(getNodeState(group), hasRelationship("persons", getNodeState(p)));
        assertThat(getNodeState(p), hasRelationship("persons", getNodeState(group)));
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


        persist(group);
        assertThat(getNodeState(group), hasRelationship("persons", getNodeState(p)));
        assertThat(getNodeState(p), hasRelationship("persons", getNodeState(group)));
    }

    @Test
    public void shouldNotCreateGraphRelationshipOutsideTransaction()
    {
        Person p = persistedPerson( "Michael", 35 );
        Person spouse = persistedPerson( "Tina", 36 );

        p.setSpouse( spouse );

        assertEquals( spouse, p.getSpouse() );
        assertThat( nodeFor( p ), hasNoRelationship("spouse", getNodeState(spouse)) );


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
        persist(p);

        assertEquals( spouse, p.getSpouse() );
        assertThat( nodeFor( p ), hasRelationship( "spouse" ) );


        Person spouse2 = persistedPerson( "Rana", 5 );
        p.setSpouse( spouse2 );
        assertEquals( spouse2, p.getSpouse() );
    }

    private Node nodeFor( Person person )
    {
        return getNodeState(person);
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
        final GraphRepository<Person> finder = neo4jTemplate.repositoryFor(Person.class);
        assertEquals( false, finder.findAll().iterator().hasNext() );
    }

}
