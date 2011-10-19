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
package org.springframework.data.neo4j.mapping;

import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.Personality;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 19.09.11
 */
public class Neo4jEntityConverterTest extends Neo4jPersistentTestBase {

    /* OUCH

    [MC]
    [GDC]->[GDB]
    [GDC]->[MC]
    [GDC]->[ESH]
    [ESH]->[MC]
    [ESH]->[GDB]
    [TRS]->[EI]
    [EI]->[ESH]
    [GDC]->[CS]
    [GDC]->[TRS]
    [GDC]->[ESH]
    [ESF]->[MC]
    [ESF]->[GDC]
    [ESF]->[FAF]
    [FAF]->[GDC]
    [GDC]->[EC]
    [EC]->[ESF]
    [EC]->[CS]
    [EC]->[EI]
    [EC]->[ESH]
    [EC]->[SST]
    [EC]->[TRS]
    [SST]->[ESF]

     */


    @Test
    public void testWriteEntityToNewNode() {
        storeInGraph(michael);
        assertEquals("Michael", michaelNode().getProperty("name"));
    }

    @Test
    public void testFindNewlyWrittenNodeInIndex() {
        storeInGraph(michael);
        final Node createdNode = michaelNode();
        final Index<Node> index = template.getIndex(Person.class, Person.NAME_INDEX);
        final Node found = index.get("name", "Michael").getSingle();
        assertEquals("node found in index", createdNode, found);
    }

    @Test
    public void testWriteEntityToExistingNode() {
        final Node existingNode = createNewNode();
        write(michael, existingNode);
        assertEquals("Entity uses provided node", (Long) existingNode.getId(), michael.getId());
        assertEquals("Michael", existingNode.getProperty("name"));
    }

    @Test
    public void testUpdateExistingNode() {
        final Node existingNode = createNewNode();
        existingNode.setProperty("name", "Test");
        assertEquals("Test", existingNode.getProperty("name"));
        write(michael, existingNode);
        assertEquals("Michael", existingNode.getProperty("name"));
        michael.setName("Emil");
        write(michael, existingNode);
        assertEquals("Emil", existingNode.getProperty("name"));
    }

    @Test
    public void testWriteConvertedPropertiesToExistingNode() {
        final Node existingNode = createNewNode();
        michael.setBirthdate(new Date(100));
        michael.setPersonality(Personality.EXTROVERT);
        write(michael, existingNode);
        assertEquals("100", existingNode.getProperty("birthdate"));
        assertEquals("EXTROVERT", existingNode.getProperty("personality"));
    }

    @Test
    public void testReadConvertedPropertiesFromExistingNode() {
        final Node existingNode = createNewNode();
        existingNode.setProperty("name", "Michael");
        existingNode.setProperty("age", 36);
        existingNode.setProperty("personality", "EXTROVERT");
        existingNode.setProperty("birthdate", "100");
        final Person p = readPerson(existingNode);
        assertEquals("Michael", p.getName());
        assertEquals(36, p.getAge());
        assertEquals(new Date(100), p.getBirthdate());
        assertEquals(Personality.EXTROVERT, p.getPersonality());
    }

    @Test
    public void testDeleteProperty() {
        final Node existingNode = createNewNode();
        write(michael, existingNode);
        michael.setName(null);
        write(michael, existingNode);
        assertEquals(false, existingNode.hasProperty("name"));
    }

    @Test
    public void testReadEntityFromExistingNode() {
        final Node node = createNewNode();
        node.setProperty("name", "Emil");
        final Person p = readPerson(node);
        assertEquals("Emil", p.getName());
    }

    @Test
    public void testSetRelationshipWithPreExistingNode() {
        Person emil1 = emil;
        storeInGraph(emil1);
        michael.setBoss(emil);
        storeInGraph(michael);
        final Node boss = getRelatedNodes(michaelNode(), "boss", Direction.INCOMING).get(0);
        assertEquals("added additional relationship end node", emil.getId(), (Long) boss.getId());
    }

    @Test
    public void testAddRelationshipWithPreExistingNode() {
        storeInGraph(michael);
        group.setPersons(singleton(michael));
        storeInGraph(group);
        final Collection<Relationship> persons = IteratorUtil.asCollection(groupNode().getRelationships(PERSONS, Direction.OUTGOING));
        final Node michaelNode = persons.iterator().next().getOtherNode(groupNode());
        assertEquals("added michaelNode to group", michael.getId(), (Long) michaelNode.getId());
    }

    @Test
    public void testAddRelationshipWithTwoExistingNodes() {
        storeInGraph(michael);
        storeInGraph(andres);
        group.setPersons(set(andres, michael));
        storeInGraph(group);
        final Collection<Node> persons = groupMemberNodes();
        assertEquals(2, persons.size());
        assertEquals(asList(michaelNode(), andresNode()), persons);
    }

    @Test
    public void testAddRelationshipWithTwoPeopleButJustOneExistingNodes() {
        storeInGraph(michael);
        group.setPersons(set(andres, michael));
        storeInGraph(group);
        final Collection<Node> persons = groupMemberNodes();
        assertEquals(2, persons.size());
        assertEquals(asList(michaelNode(), andresNode()), persons);
    }


    @Test
    public void testAddRelationshipCascadeOverTwoSteps() {
        andres.setBoss(emil);
        group.setPersons(singleton(andres));
        storeInGraph(group);
        final Collection<Node> persons = groupMemberNodes();
        assertEquals(1, persons.size());
        assertEquals(asList(andresNode()), persons);
        assertEquals(emil.getId(), (Long) getRelatedNodes(andresNode(), "boss", Direction.INCOMING).get(0).getId());
    }

    @Test
    public void testDeleteSingleRelationship() {
        emil.setBoss(andres);
        storeInGraph(emil);

        emil.setBoss(null);
        storeInGraph(emil);

        assertEquals(0, getRelatedNodes(emilNode(), "boss", Direction.INCOMING).size());
    }

    @Test
    public void testDeleteMultipleRelationships() {
        group.setPersons(set(storeInGraph(emil), storeInGraph(michael), storeInGraph(andres)));
        storeInGraph(group);

        group.getPersons().remove(emil);
        storeInGraph(group);

        assertEquals(set(andresNode(), michaelNode()), set(groupMemberNodes()));
    }

    @Test
    public void testReadRelationshipCollectionFromGraph() {
        Node groupNode = createNewNode();
        Node p1 = createNewNode();
        Node p2 = createNewNode();
        groupNode.createRelationshipTo(p1, PERSONS);
        groupNode.createRelationshipTo(p2, PERSONS);

        Group g = readGroup(groupNode);
        assertEquals(set(readPerson(p1), readPerson(p2)), set(g.getPersons()));
    }

    @Test
    public void testReadRelationshipIterableFromGraph() {
        Node groupNode = createNewNode();
        Node p1 = createNewNode();
        Node p2 = createNewNode();
        groupNode.createRelationshipTo(p1, PERSONS);
        groupNode.createRelationshipTo(p2, PERSONS);

        Group g = readGroup(groupNode);
        assertEquals(set(readPerson(p1), readPerson(p2)), set(g.getReadOnlyPersons()));
    }

    @Test
    public void testRelationshipCollectionModificationIsReflectedInGraph() {
        group.setPersons(set(storeInGraph(emil), storeInGraph(andres)));
        storeInGraph(group);

        group.getPersons().remove(emil);
        group.getPersons().add(storeInGraph(michael));
        storeInGraph(group);

        assertEquals(set(andresNode(), michaelNode()), set(groupMemberNodes()));
    }

    @Test
    public void testNullValuesForRelationshipCollectionsAreIgnored() {
        group.setPersons(set(storeInGraph(emil)));
        storeInGraph(group);
        assertEquals(set(emilNode()), set(groupMemberNodes()));
        group.setPersons(null);
        storeInGraph(group);
        assertEquals(set(emilNode()), set(groupMemberNodes()));
    }

    @Test
    public void testSetRelationshipWithNonExistingNode() {
        michael.setBoss(emil);
        storeInGraph(michael);
        final Node node = michaelNode();
        final Node boss = getRelatedNodes(node, "boss", Direction.INCOMING).get(0);
        assertEquals("added additional relationship end node", emil.getId(), (Long) boss.getId());
    }

    @Test
    public void testAddRelationshipWithNonExistingNode() {
        group.setPersons(singleton(michael));
        storeInGraph(group);
        final Node groupNode = groupNode();
        final Node michaelNode = getRelatedNodes(groupNode, "persons", Direction.OUTGOING).get(0);
        assertEquals("added member to group", michael.getId(), (Long) michaelNode.getId());
    }


    @Test
    public void testCascadingReadWithProperties() {
        Node groupNode = createNewNode();
        Node julianNode = createNewNode();
        julianNode.setProperty("name", "Julian");
        groupNode.createRelationshipTo(julianNode, PERSONS);

        Group g = readGroup(groupNode);
        Person julian = IteratorUtil.first(g.getPersons());
        assertEquals("Julian", julian.getName());
    }

    @Test
    public void testLoadFriendShipsFromPersons() throws Exception {
        storeInGraph(michael);
        storeInGraph(andres);

        Relationship friendshipRelationship = makeFriends(michaelNode(), andresNode(), 19);

        Person m = readPerson(michaelNode());
        Friendship friendship = IteratorUtil.first(m.getFriendships());

        assertEquals((Long) friendshipRelationship.getId(), friendship.getId());
        assertEquals(19, friendship.getYears());
        assertEquals(friendship.getPerson1(), michael);
        assertEquals(friendship.getPerson2(), andres);
    }

}
