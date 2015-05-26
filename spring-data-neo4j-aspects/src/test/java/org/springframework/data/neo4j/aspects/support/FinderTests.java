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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class FinderTests extends EntityTestBase {

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
        testTeam.createSDGTeam();
        Iterable<Person> teamMembers = personRepository.findAllTeamMembers(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael,testTeam.david,testTeam.emil));
    }

    @Test
    @Transactional
    public void testFindPersonWithQueryAnnotation() {
        testTeam.createSDGTeam();
        Person boss = personRepository.findBoss(testTeam.michael);
        assertThat(boss, is(testTeam.emil));
    }
    @SuppressWarnings("unchecked")
    @Test
    @Transactional
    public void testFindIterableMapsWithQueryAnnotation() {
        final TestTeam testTeam = new TestTeam(neo4jTemplate);
        testTeam.createSDGTeam();
        Iterable<Map<String,Object>> teamMembers = personRepository.findAllTeamMemberData(testTeam.sdg);
        assertThat(asCollection(teamMembers), hasItems(testTeam.simpleRowFor(testTeam.michael,"member"),testTeam.simpleRowFor(testTeam.david,"member"),testTeam.simpleRowFor(testTeam.emil,"member")));
    }

    @Test
    @Transactional
    public void testFindByNamedQuery() {
        final TestTeam testTeam = new TestTeam(neo4jTemplate);
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
        assertEquals("persisted person 1", true, hasPersistentState(p1));
        assertEquals("persisted person 2", true, hasPersistentState(p2));
        assertThat(asCollection(personRepository.findAll()), hasItems(p2, p1));
    }

    @Test
    @Transactional
    public void testSavePerson() {
        Person p1 = new Person("Michael", 35);
        personRepository.save(p1);
        assertEquals("persisted person", true, hasPersistentState(p1));
        assertThat(personRepository.findOne(p1.getId()), is(p1));
    }
    @Test
    public void testDeletePerson() {
        Person p1 = persistedPerson("Michael", 35);
        personRepository.delete(p1);
        try (Transaction tx = graphDatabaseService.beginTx()) {
            assertEquals("people deleted", false, personRepository.findAll().iterator().hasNext());
            tx.success();
        }
    }
    @Test
    public void testDeletePeople() {
        Person p1 = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 26);
        personRepository.delete(asList(p1,p2));
        try (Transaction tx = graphDatabaseService.beginTx()) {
            assertEquals("people deleted", false, personRepository.findAll().iterator().hasNext());
            tx.success();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED  )
    public void testFindRelationshipEntity() {
        Friendship friendship;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Person p1 = persistedPerson("Michael", 35);
            Person p2 = persistedPerson("David", 27);
            friendship = p1.knows(p2);
            tx.success();
        }
        try (Transaction tx = graphDatabaseService.beginTx()) {
            assertEquals("Wrong friendship count.", 1L, (long) friendshipRepository.count());
            assertEquals(friendship, friendshipRepository.findOne(getRelationshipId(friendship)));
            assertEquals("Did not find friendship.", Collections.singleton(friendship), new HashSet<Friendship>(IteratorUtil.asCollection(friendshipRepository.findAll())));
            tx.success();
        }
    }

    @Test
    @Transactional
    public void testFinderFindById() {
        Person p = persistedPerson("Michael", 35);
        Person pById = personRepository.findOne(getNodeId(p));
        assertEquals(p, pById);
    }

    @Test
    @Transactional
    public void testExists() {
        Person p = persistedPerson("Michael", 35);
        boolean found = personRepository.exists(getNodeId(p));
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
        Group g = persist(new Group());
        g.setName("test");
        Group g2 = persist(new Group());
        g.setName("test");
        Collection<Group> groups = IteratorUtil.addToCollection(groupRepository.findAll().iterator(), new HashSet<Group>());
        assertEquals(2, groups.size());
	}
    @Test
	@Transactional
	public void testFindPaged() {
	    log.debug("FindAllOnGroup start");
        Person p1=Person.persistedPerson("person1",11);
        Person p2=Person.persistedPerson("person2", 12);
        Person p3=Person.persistedPerson("person3", 13);
        final List<Person> all = IteratorUtil.addToCollection(personRepository.findAll(), new ArrayList<Person>());
        final Page<Person> page0 = personRepository.findAll(new PageRequest(0, 2));
        final Page<Person> page1 = personRepository.findAll(new PageRequest(1, 2));
        final Page<Person> page2 = personRepository.findAll(new PageRequest(2, 2));
        assertPage(page0, 0, 2, 3, all.get(0), all.get(1));
        assertPage(page1, 1, 2, 3, all.get(2));
        assertPage(page2, 2, 2, 3);
	}

    private void assertPage(Page<Person> page0, int pageNumber, int totalPages, final int totalElements, Person... people) {
        assertEquals("content count",people.length,page0.getNumberOfElements());
        assertEquals("page number",pageNumber,page0.getNumber());
        assertEquals("page size",2,page0.getSize());
        assertEquals("total elements", totalElements,page0.getTotalElements());
        assertEquals("page count",totalPages,page0.getTotalPages());
        assertEquals("next page",pageNumber < totalPages-1,page0.hasNext());
        assertEquals("previous page",pageNumber > 0,page0.hasPrevious());
        assertEquals("page content",asList(people),page0.getContent());
    }
}
