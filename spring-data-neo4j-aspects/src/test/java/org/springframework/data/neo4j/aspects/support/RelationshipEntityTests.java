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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.template.GraphCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.*;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})

public class RelationshipEntityTests extends EntityTestBase {

    @Test
    @Transactional
    public void testRelationshipCreate() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        Relationship rel = getNodeState(p).getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
        assertEquals(getRelationshipState(f), rel);
        assertEquals(getNodeState(p2), rel.getEndNode());
    }

    @Test
    @Transactional
    public void shouldNotCreateSameRelationshipTwice() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        Friendship f2 = p.knows(p2);
        assertEquals(f, f2);
        assertEquals(1, IteratorUtil.count(p.getFriendships()));
    }

    @Test
    @Transactional
    public void shouldSupportSetOfRelationshipEntities() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        final Friendship friendship = p.knows(p2);
        final Set<Friendship> result = p.getFriendshipsSet();
        assertEquals(1, IteratorUtil.count(result));
        assertEquals(friendship,IteratorUtil.first(result));
    }

    @Test
    @Transactional
    public void shouldSupportManagedSetAddOfRelationshipEntities() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        final Set<Friendship> friends = p.getFriendshipsSet();
        assertEquals(0, IteratorUtil.count(friends));
        final Friendship friendship = new Friendship(p, p2, 10);
        friends.add(friendship);
        assertEquals(1, IteratorUtil.count(friends));
        assertEquals(friendship,IteratorUtil.first(friends));
        assertEquals(friendship.getYears(),IteratorUtil.first(friends).getYears());
    }

    @Test
    @Transactional
    public void shouldSupportManagedSetAddOfRelationshipEntitiesSave() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        final Set<Friendship> friends = p.getFriendshipsSet();
        assertEquals(0, IteratorUtil.count(friends));
        final Friendship friendship = new Friendship(p, p2, 10);
        Friendship friends2 = neo4jTemplate.save(friendship);
        assertEquals(friendship, friends2);
        assertEquals(friendship.getYears(),friends2.getYears());
        neo4jTemplate.fetch(p);
        Set<Friendship> friendships = neo4jTemplate.fetch(p.getFriendshipsSet());
        Friendship friends3 = IteratorUtil.first(friendships);
        assertEquals(friendship, friends3);
        assertEquals(friendship.getYears(),friends3.getYears());
    }

    @Test
    @Transactional
    public void shouldSupportManagedSetRemoveOfRelationshipEntities() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        final Friendship friendship = p.knows(p2);
        final Set<Friendship> friends = p.getFriendshipsSet();
        assertEquals(1, friends.size());
        assertEquals(friendship,IteratorUtil.first(friends));
        friends.remove(friendship);
        assertEquals(0, IteratorUtil.count(friends));
    }

    @Test
    @Transactional
    public void testRelationshipSetProperty() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setYears(1);
        assertEquals(1, getRelationshipState(f).getProperty("Friendship.years"));
    }

    @Test
    @Transactional
    public void testRelationshipSetPropertyLater() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = new Friendship(p,p2,1);
        f = neo4jTemplate.save(f);
        Relationship r = getRelationshipState(f);
        assertEquals(1, r.getProperty("Friendship.years"));
        Friendship f2 = neo4jTemplate.findOne(r.getId(), Friendship.class);
        f2.setYears(2);
        f2 = neo4jTemplate.save(f2);
        assertEquals(2, getRelationshipState(f2).getProperty("Friendship.years"));
    }

    @Test
    @Transactional
    public void testRelationshipGetProperty() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        getRelationshipState(f).setProperty("Friendship.years", 1);
        assertEquals(1, f.getYears());
    }

    @Test
    @Transactional
    public void testRelationshipGetStartNodeAndEndNode() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(p, f.getPerson1());
        assertEquals(p2, f.getPerson2());
    }

    @Test
    @Transactional
    public void testGetRelationshipToReturnsRelationship() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(f, neo4jTemplate.getRelationshipBetween(p, p2, Friendship.class, "knows"));
    }
    
    @Test
    @Transactional
    public void testGetRelationshipTo() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertNotNull(p.getRelationshipTo(p2, "knows"));
    }

    @Test
    public void testRemoveRelationshipEntity() {
        cleanDb();
        Friendship f;
        try (Transaction tx = graphDatabaseService.beginTx())
        {
            Person p = persistedPerson("Michael", 35);
            Person p2 = persistedPerson("David", 25);
            f = p.knows(p2);
            tx.success();
        }
        try (Transaction tx = graphDatabaseService.beginTx())
        {
            neo4jTemplate.delete(f);
            tx.success();
        }
        try (Transaction tx = graphDatabaseService.beginTx()) {
            assertFalse("Unexpected relationship entity found.", friendshipRepository.findAll().iterator().hasNext());
            tx.success();
        }
    }

    @Test
    public void testRemoveRelationshipEntityIfNodeEntityIsRemoved() {
        cleanDb();
        Person p;
        try (Transaction tx = graphDatabaseService.beginTx())
        {
            p = persistedPerson("Michael", 35);
            Person p2 = persistedPerson("David", 25);
            p.knows(p2);
            tx.success();
        }
        try (Transaction tx = graphDatabaseService.beginTx())
        {
            neo4jTemplate.delete(p);
            tx.success();
        }
        try (Transaction tx = graphDatabaseService.beginTx()) {
            assertFalse("Unexpected relationship entity found.", friendshipRepository.findAll().iterator().hasNext());
            tx.success();
        }
    }
}
