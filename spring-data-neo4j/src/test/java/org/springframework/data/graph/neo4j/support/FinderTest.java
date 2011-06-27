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

package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.*;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:repository-namespace-config-context.xml"})
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class FinderTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testFinderFindAll() {
        Person p1 = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Iterable<Person> allPersons = personRepository.findAll();
        assertThat(asCollection(allPersons), hasItems(p1, p2));
    }

    @Test
    @Transactional
    public void testFindIterableOfPersonWithQueryAnnotation() {
        final TestTeam testTeam = new TestTeam();
        testTeam.createSDGTeam();
        Iterable<Person> teamMembers = personRepository.findAllTeamMembers(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael,testTeam.david,testTeam.emil));
    }

    @Test
    @Transactional
    public void testFindPersonWithQueryAnnotation() {
        final TestTeam testTeam = new TestTeam();
        testTeam.createSDGTeam();
        Person boss = personRepository.findBoss(testTeam.michael);
        assertThat(boss, is(testTeam.emil));
    }
    @Test
    @Transactional
    public void testFindIterableMapsWithQueryAnnotation() {
        final TestTeam testTeam = new TestTeam();
        testTeam.createSDGTeam();
        Iterable<Map<String,Object>> teamMembers = personRepository.findAllTeamMemberData(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.simpleRowFor(testTeam.michael,"member"),testTeam.simpleRowFor(testTeam.david,"member"),testTeam.simpleRowFor(testTeam.emil,"member")));
    }

    @Test
    @Transactional
    public void testFindByNamedQuery() {
        final TestTeam testTeam = new TestTeam();
        testTeam.createSDGTeam();
        Group team = personRepository.findTeam(testTeam.michael);
        assertThat(team, is(testTeam.sdg));
    }

    @Test
    @Transactional
    public void testSaveManyPeople() {
        Person p1 = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        personRepository.save(asList(p1,p2));
        assertEquals("persisted person 1",true,p1.hasPersistentState());
        assertEquals("persisted person 2",true,p2.hasPersistentState());
        assertThat(asCollection(personRepository.findAll()), hasItems(p2, p1));
    }

    @Test
    @Transactional
    public void testSavePerson() {
        Person p1 = new Person("Michael", 35);
        personRepository.save(p1);
        assertEquals("persisted person",true,p1.hasPersistentState());
        assertThat(personRepository.findOne(p1.getId()), is(p1));
    }
    @Test
    public void testDeletePerson() {
        Person p1 = persistedPerson("Michael", 35);
        personRepository.delete(p1);
        assertEquals("people deleted", false, personRepository.findAll().iterator().hasNext());
    }
    @Test
    public void testDeletePeople() {
        Person p1 = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 26);
        personRepository.delete(asList(p1,p2));
        assertEquals("people deleted", false, personRepository.findAll().iterator().hasNext());
    }

    @Test
    @Transactional
    public void testFindRelationshipEntity() {
        Person p1 = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 27);
        Friendship friendship = p1.knows(p2);
        assertEquals("Wrong friendship count.", 1L, (long) friendshipRepository.count());
        assertEquals(friendship, friendshipRepository.findOne(friendship.getRelationshipId()));
        assertEquals("Did not find friendship.", Collections.singleton(friendship), new HashSet<Friendship>(IteratorUtil.asCollection(friendshipRepository.findAll())));
    }

    @Test
    @Transactional
    public void testFinderFindById() {
        Person p = persistedPerson("Michael", 35);
        Person pById = personRepository.findOne(p.getNodeId());
        assertEquals(p, pById);
    }

    @Test
    @Transactional
    public void testExists() {
        Person p = persistedPerson("Michael", 35);
        boolean found = personRepository.exists(p.getNodeId());
        assertTrue("Found persisted entity", found);
    }
    @Test
    @Transactional
    public void testDoesntExist() {
        boolean found = personRepository.exists(Long.MAX_VALUE-1);
        assertFalse("Non existend id isn't foundpo ", found);
    }

    @Test
    @Transactional
    public void testFinderFindByIdNonexistent() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = personRepository.findOne(589736218L);
        Assert.assertNull(p2);
    }

    @Test
    @Transactional
    public void testFinderCount() {
        assertEquals(0L, personRepository.count());
        Person p = persistedPerson("Michael", 35);
        assertEquals(1L, personRepository.count());
    }

    @Test
	@Transactional
	public void testFindAllOnGroup() {
	    log.debug("FindAllOnGroup start");
        Group g = new Group().persist();
        g.setName("test");
        Group g2 = new Group().persist();
        g.setName("test");
        Collection<Group> groups = IteratorUtil.addToCollection(groupRepository.findAll().iterator(), new HashSet<Group>());
        Assert.assertEquals(2, groups.size());
	    log.debug("FindAllOnGroup done");
	}
}
