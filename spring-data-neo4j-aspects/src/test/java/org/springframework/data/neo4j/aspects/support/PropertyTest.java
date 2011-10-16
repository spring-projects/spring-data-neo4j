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
import org.neo4j.graphdb.NotFoundException;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.Personality;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml"})

public class PropertyTest extends EntityTestBase {

    @Test
    @Transactional
    public void testSetPropertyEnum() {
        Person p = persistedPerson("Michael", 35);
        p.setPersonality(Personality.EXTROVERT);
        assertEquals("Wrong enum serialization.", "EXTROVERT", getNodeState(p).getProperty("personality"));
    }

    @Test
    @Transactional
    public void testGetPropertyEnum() {
        Person p = persistedPerson("Michael", 35);
        getNodeState(p).setProperty("personality", "EXTROVERT");
        assertEquals("Did not deserialize property value properly.", Personality.EXTROVERT, p.getPersonality());
    }

    @Test(expected = NotFoundException.class)
    @Transactional
    public void testSetTransientPropertyFieldNotManaged() {
        Person p = persistedPerson("Michael", 35);
        p.setThought("food");
        getNodeState(p).getProperty("thought");
    }

    @Test
    @Transactional
    public void testGetTransientPropertyFieldNotManaged() {
        Person p = persistedPerson("Michael", 35);
        p.setThought("food");
        getNodeState(p).setProperty("thought", "sleep");
        assertEquals("Should not have read transient value from graph.", "food", p.getThought());
    }
    @Test
    @Transactional
    @Rollback(false)
    public void testRelationshipSetPropertyDate() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setFirstMeetingDate(new Date(3));
        assertEquals("Date not serialized properly.", "3", getRelationshipState(f).getProperty("Friendship.firstMeetingDate"));
    }

    @Test
    @Transactional
    public void testRelationshipGetPropertyDate() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        getRelationshipState(f).setProperty("Friendship.firstMeetingDate", "3");
        assertEquals("Date not deserialized properly.", new Date(3), f.getFirstMeetingDate());
    }

    @Test(expected = NotFoundException.class)
    @Transactional
    public void testRelationshipSetTransientPropertyFieldNotManaged() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setLatestLocation("Menlo Park");
        getRelationshipState(f).getProperty("Friendship.latestLocation");
    }

    @Test
    @Transactional
    public void testRelationshipGetTransientPropertyFieldNotManaged() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setLatestLocation("Menlo Park");
        getRelationshipState(f).setProperty("Friendship.latestLocation", "Palo Alto");
        assertEquals("Should not have read transient value from graph.", "Menlo Park", f.getLatestLocation());
    }

    @Test
    @Transactional
    public void testEntityIdField() {
        Person p = persistedPerson("Michael", 35);
        assertEquals("Wrong ID.", getNodeState(p).getId(), p.getId());
    }

    @Test
    @Transactional
    public void testRelationshipIdField() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
		assertEquals("Wrong ID.", (Long) getRelationshipState(f).getId(), getRelationshipId(f));
    }
}
