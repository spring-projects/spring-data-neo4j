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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Mentorship;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})

public class NodeEntityRelationshipTests extends EntityTestBase {

    @Test
    @Transactional
    public void testCreateRelationshipWithoutAnnotationOnSet() {
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        Node spouseNode= getNodeState(p).getSingleRelationship(DynamicRelationshipType.withName("spouse"), Direction.OUTGOING).getEndNode();
        assertEquals(getNodeState(spouse), spouseNode);
        assertEquals(spouse, p.getSpouse());
    }

    @Test
    @Transactional
    public void testCreateRelationshipWithAnnotationOnSet() {
        Person p = persistedPerson("Michael", 35);
        Person mother = persistedPerson("Gabi", 60);
        p.setMother(mother);
        Node motherNode = getNodeState(p).getSingleRelationship(DynamicRelationshipType.withName("mother"), Direction.OUTGOING).getEndNode();
        assertEquals(getNodeState(mother), motherNode);
        assertEquals(mother, p.getMother());
    }

    @Test
    @Transactional
    public void testDeleteRelationship() {
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        p.setSpouse(null);
        Assert.assertNull(getNodeState(p).getSingleRelationship(DynamicRelationshipType.withName("spouse"), Direction.OUTGOING));
        Assert.assertNull(p.getSpouse());
    }

    @Test
    @Transactional
    public void testDeletePreviousRelationshipOnNewRelationship() {
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        Person friend = persistedPerson("Helga", 34);
        p.setSpouse(spouse);
        p.setSpouse(friend);
        assertEquals(getNodeState(friend), getNodeState(p).getSingleRelationship(DynamicRelationshipType.withName("spouse"), Direction.OUTGOING).getEndNode());
        assertEquals(friend, p.getSpouse());
    }

    @Test
    @Transactional
    public void testCreateIncomingRelationshipWithAnnotationOnSet() {
        Person p = persistedPerson("David", 25);
        Person boss = persistedPerson("Emil", 32);
        p.setBoss(boss);
        assertEquals(getNodeState(boss), getNodeState(p).getSingleRelationship(DynamicRelationshipType.withName("boss"), Direction.INCOMING).getStartNode());
        assertEquals(boss, p.getBoss());
    }

    @Transactional
    public void testAllowsCircularRelationship() {
        Person p = persistedPerson("Michael", 35);
        p.setBoss(p);

        assertEquals("created self-referencing relationship",p,p.getBoss());
    }
    @Test
    @Transactional
    public void testSetOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = persist(new Group());
        Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
        group.setPersons(persons);
        Relationship michaelRel = getNodeState(michael).getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.INCOMING);
        Relationship davidRel = getNodeState(david).getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.INCOMING);
        assertEquals(getNodeState(group), michaelRel.getStartNode());
        assertEquals(getNodeState(group), davidRel.getStartNode());
    }

    @Test
    @Transactional
    public void testGetOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = persist(new Group());
        Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
        group.setPersons(persons);
        Collection<Person> personsFromGet = group.getPersons();
        assertEquals(persons, personsFromGet);
        assertTrue(Set.class.isAssignableFrom(personsFromGet.getClass()));
    }

    @Test
    @Transactional
    public void testAddToOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = persist(new Group());
        group.setPersons(new HashSet<Person>());
        group.getPersons().add(michael);
        group.getPersons().add(david);
        Collection<Person> personsFromGet = group.getPersons();
        assertEquals(new HashSet<Person>(Arrays.asList(david,michael)), personsFromGet);
        assertTrue(Set.class.isAssignableFrom(personsFromGet.getClass()));
    }

    @Test
    public void testAddToOneToManyRelationshipOutsideOfTransaction() {
        Person michael = persistedPerson("Michael", 35);
        Group group = persist(new Group());
        group.getPersons().add(michael);
        group = persist(group);
        Collection<Person> personsFromGet = group.getPersons();
        assertEquals(new HashSet<Person>(Arrays.asList(michael)), personsFromGet);
    }

    @Test
    @Transactional
    public void testRemoveFromOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = persist(new Group());
        group.setPersons(new HashSet<Person>(Arrays.asList(michael, david)));
        group.getPersons().remove(david);
        assertEquals(Collections.singleton(michael), group.getPersons());
    }
    @Test
    @Transactional
    public void testRemoveAllFromOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = persist(new Group());
        group.setPersons(new HashSet<Person>(Arrays.asList(michael, david)));
        group.getPersons().removeAll(Collections.singleton(david));
        assertEquals(Collections.singleton(michael), group.getPersons());
    }
    @Test
    @Transactional
    public void testRetainAllFromOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = persist(new Group());
        group.setPersons(new HashSet<Person>(Arrays.asList(michael, david)));
        group.getPersons().retainAll(Collections.singleton(david));
        assertEquals(Collections.singleton(david), group.getPersons());
    }
    @Test
    @Transactional
    public void testClearFromOneToManyRelationship() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = persist(new Group());
        group.setPersons(new HashSet<Person>(Arrays.asList(michael, david)));
        group.getPersons().clear();
        assertEquals(Collections.<Person>emptySet(), group.getPersons());
    }


    @Test
    @Transactional
    public void testRelationshipGetEntities() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Person p3 = persistedPerson("Emil", 32);
        Friendship f2 = p.knows(p2);
        Friendship f3 = p.knows(p3);
        assertEquals(new HashSet<Friendship>(Arrays.asList(f2, f3)), IteratorUtil.addToCollection(p.getFriendships().iterator(), new HashSet<Friendship>()));
    }

    @Test(expected = InvalidDataAccessApiUsageException.class)
    @Transactional
    public void testRelationshipSetEntitiesShouldThrowException() {
        Person p = persistedPerson("Michael", 35);
        p.setFriendships(new HashSet<Friendship>());
    }

    @Test
    @Transactional
    public void testOneToManyReadOnly() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Group group = persist(new Group());
        Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
        group.setPersons(persons);
        assertEquals(persons, IteratorUtil.addToCollection(group.getReadOnlyPersons().iterator(), new HashSet<Person>()));
    }

    @Test
    @Transactional
    public void multipleRelationshipsOfSameTypeBetweenTwoEntities() {
        Person michael = persistedPerson("Michael", 35);
        Person david = persistedPerson("David", 25);
        Friendship friendship1 = neo4jTemplate.createRelationshipBetween(michael, david, Friendship.class, "knows", true);
        friendship1.setYears(1);
        Friendship friendship2 = neo4jTemplate.createRelationshipBetween(michael, david, Friendship.class, "knows", true);
        friendship2.setYears(2);
        assertTrue("two different relationships", friendship1 != friendship2);
        assertTrue("two different relationships", getRelationshipState(friendship1) != getRelationshipState(friendship2));
        assertEquals(1, friendship1.getYears());
        assertEquals(2,friendship2.getYears());
        final Collection<Relationship> friends = IteratorUtil.asCollection(getNodeState(michael).getRelationships(Direction.OUTGOING, DynamicRelationshipType.withName("knows")));
        assertEquals(2,friends.size());
        assertTrue(friends.contains(getRelationshipState(friendship1)));
        assertTrue(friends.contains(getRelationshipState(friendship2)));
    }


    @Test(expected = InvalidDataAccessApiUsageException.class)
    @Transactional
    public void testOneToManyReadOnlyShouldThrowExceptionOnSet() {
        Group group = persist(new Group());
        group.setReadOnlyPersons(new HashSet<Person>());
    }

    @Test
    @Transactional
    public void testSingleRelatedToViaField() {
        Group group = persist(new Group());
        Person mentor = persist(new Person());
        group.setMentorship(new Mentorship(mentor,group));
        persist(group);
        final Node node = neo4jTemplate.getPersistentState(group);
        assertEquals(1,IteratorUtil.count(node.getRelationships(Direction.INCOMING,DynamicRelationshipType.withName("mentors"))));
        final Group loaded = neo4jTemplate.load(node, Group.class);
        assertEquals(group.getMentorship(),loaded.getMentorship());
        assertEquals(group.getMentorship().getId(),loaded.getMentorship().getId());
        assertEquals(mentor, group.getMentorship().getMentor());
        assertEquals(group, group.getMentorship().getGroup());
    }
    
    @Test
    @Transactional
    public void testRemoveSingleRelatedToViaField() {
        Group group = persist(new Group());
        Person mentor = persist(new Person());
        group.setMentorship(new Mentorship(mentor,group));
        persist(group);
        group.setMentorship(null);
        persist(group);
        final Node node = neo4jTemplate.getPersistentState(group);
        assertEquals(0,IteratorUtil.count(node.getRelationships(Direction.INCOMING,DynamicRelationshipType.withName("mentors"))));
        final Group loaded = neo4jTemplate.load(node, Group.class);
        assertThat(loaded.getMentorship(), is(nullValue()));
    }
    @Test
    @Transactional
    public void testUpdateSingleRelatedToViaField() {
        Group group = persist(new Group());
        group.setMentorship(new Mentorship(persist(new Person()),group));
        persist(group);
        final Long firstMentorshipId = group.getMentorship().getId();
        final Person mentor2 = new Person();
        group.setMentorship(new Mentorship(persist(mentor2),group));
        persist(group);
        final Node node = neo4jTemplate.getPersistentState(group);
        assertEquals(1,IteratorUtil.count(node.getRelationships(Direction.INCOMING,DynamicRelationshipType.withName("mentors"))));
        final Group loaded = neo4jTemplate.load(node, Group.class);
        assertFalse(loaded.getMentorship().getId().equals(firstMentorshipId));
        assertEquals(mentor2, group.getMentorship().getMentor());
        assertEquals(group, group.getMentorship().getGroup());
    }
}
