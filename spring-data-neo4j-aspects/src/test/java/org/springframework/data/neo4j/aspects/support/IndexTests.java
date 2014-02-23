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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.SubGroup;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.springframework.data.neo4j.aspects.Person.NAME_INDEX;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD, hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)

public class IndexTests extends EntityTestBase {

    private static final String NAME = "name";
    private static final String NAME_VALUE = "aName";
    private static final String NAME_VALUE2 = "aSecondName";
    private static final String NAME_VALUE3 = "aThirdName";

    @Test
    @Transactional
    @Ignore
    public void testCanIndexIntFieldsOnRelationshipEntities() {
        Person p = persistedPerson(NAME_VALUE, 35);
        Person p2 = persistedPerson(NAME_VALUE2, 25);
        Friendship friendship = p.knows(p2);
        friendship.setYears(1);
        GraphRepository<Friendship> friendshipFinder = neo4jTemplate.repositoryFor(Friendship.class);
        assertEquals(friendship, friendshipFinder.findByPropertyValue("Friendship.years", 1));
    }


    @Test
    @Transactional
    public void testGetRelationshipFromLookedUpNode() {
        Person me = persistedPerson(NAME_VALUE, 35);
        Person spouse = persistedPerson(NAME_VALUE3, 36);
        me.setSpouse(spouse);
        final Person foundMe = this.personRepository.findByPropertyValue(NAME_INDEX, "name", NAME_VALUE);
        assertEquals(spouse, foundMe.getSpouse());
    }

    @Test
    //@Transactional
    //@Ignore("remove property from index not workin")
    public void testRemovePropertyFromIndex() {
        try (Transaction tx = neo4jTemplate.getGraphDatabase().beginTx()) {
            Group group = persist(new Group());
            group.setName(NAME_VALUE);
            getGroupIndex().remove(getNodeState(group), NAME);
            tx.success();
        }
        final Group found = this.groupRepository.findByPropertyValue(NAME, NAME_VALUE);
        assertNull("Group.name removed from index", found);
    }

    @Test
    //@Transactional
    //@Ignore("remove property from index not workin")
    public void testRemoveNodeFromIndex() {
        try (Transaction tx = neo4jTemplate.getGraphDatabase().beginTx()) {
            Group group = persist(new Group());
            group.setName(NAME_VALUE);
            getGroupIndex().remove(getNodeState(group));
            tx.success();
        }
        final Group found = this.groupRepository.findByPropertyValue(NAME, NAME_VALUE);
        assertNull("Group.name removed from index", found);
    }

    private Index<Node> getGroupIndex() {
        return neo4jTemplate.getIndex(Group.class);
    }

    @Test
    @Transactional
    public void testFindGroupByIndex() {
        Group group = persist(new Group());
        group.setName(NAME_VALUE);
        final Group found = this.groupRepository.findByPropertyValue(NAME, NAME_VALUE);
        assertEquals(group, found);
    }

    @Test
    @Transactional
    @Ignore
    public void testFindGroupByInstanceIndex() {
        Group group = persist(new SubGroup());
        group.setIndexLevelName("indexLevelNameValue");
        Index<Node> subGroupIndex = neo4jTemplate.getIndex(SubGroup.class);
        final Node found = subGroupIndex.get("indexLevelName", "indexLevelNameValue").getSingle();
        final SubGroup foundEntity = neo4jTemplate.createEntityFromState(found, SubGroup.class, neo4jTemplate.getMappingPolicy(SubGroup.class));
        assertEquals(group, foundEntity);
    }

    @Test
    @Transactional
    public void testFindGroupByAlternativeFieldNameIndex() {
        Group group = persist(new Group());
        group.setOtherName(NAME_VALUE);
        final Group found = this.groupRepository.findByPropertyValue(Group.OTHER_NAME_INDEX, NAME_VALUE);
        assertEquals(group, found);
    }

    @Test
    @Transactional
    @Ignore
    public void testDontFindGroupByNonIndexedFieldWithAnnotation() {
        Group group = persist(new Group());
        group.setUnindexedName("value-unindexedName");
        final Group found = this.groupRepository.findByPropertyValue("unindexedName", "value-unindexedName");
        assertNull(found);
    }

    @Test
    @Transactional
    public void testDontFindGroupByNonIndexedField() {
        Group group = persist(new Group());
        group.setUnindexedName2("value-unindexedName2");
        final Group found = this.groupRepository.findByPropertyValue("unindexedName2", "value-unindexedName2");
        assertNull(found);
    }

    @Test
    @Transactional
    public void testFindAllGroupsByIndex() {
        Group group = persist(new Group());
        group.setName(NAME_VALUE);
        Group group2 = persist(new Group());
        group2.setName(NAME_VALUE);
        final Iterable<Group> found = this.groupRepository.findAllByPropertyValue(NAME, NAME_VALUE);
        final Collection<Group> result = IteratorUtil.addToCollection(found.iterator(), new HashSet<Group>());
        assertEquals(new HashSet<>(Arrays.asList(group, group2)), result);
    }

    @Test
    @Transactional
    public void testFindAllGroupsByNonNumericIndexedNumber() {
        final Group group = new Group();
        final byte value = (byte) 100;
        group.setSecret(value);
        groupRepository.save(group);
        final PropertyContainer node = neo4jTemplate.getPersistentState(group);
        final Iterable<Group> found = this.groupRepository.findAllByPropertyValue("secret", value);
        assertEquals(1, IteratorUtil.count(found));
        final Node foundWithTemplate = neo4jTemplate.lookup("Group","secret", value).to(Node.class).singleOrNull();
        assertEquals(node, foundWithTemplate);
        final Node foundGroup = neo4jTemplate.getGraphDatabaseService().index().forNodes("Group").get("secret", value).getSingle();
        assertEquals(node, foundGroup);
    }

    @Test
    @Transactional
    public void shouldFindGroupyByQueryString() {
        Group group = persist(new Group());
        group.setFullTextName("queryableName");
        final Iterable<Group> found = groupRepository.findAllByQuery(Group.SEARCH_GROUPS_INDEX, "fullTextName", "queryable*");
        final Collection<Group> result = IteratorUtil.addToCollection(found.iterator(), new HashSet<Group>());
        assertEquals(new HashSet<>(Arrays.asList(group)), result);
    }

    @Test
    @Transactional
    public void testFindAllPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final Person found = personRepository.findByPropertyValue(NAME_INDEX, "name", NAME_VALUE);
        assertEquals(person, found);
    }

    @Test
    public void findsPersonByIndexOnAnnotatedIntFieldInSeparateTransactions() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final Person found = this.personRepository.findByPropertyValue("age", 35);
        assertEquals("person found inside range", person, found);
    }

    @Test
    @Transactional
    public void testRangeQueryPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final Person found = this.personRepository.findAllByRange("age", 10, 40).iterator().next();
        assertEquals("person found inside range", person, found);
    }

    @Test
    @Transactional
    public void testOutsideRangeQueryPersonByIndexOnAnnotatedField() {
        persistedPerson(NAME_VALUE, 35);
        Iterable<Person> emptyResult = this.personRepository.findAllByRange("age", 0, 34);
        assertFalse("nothing found outside range", emptyResult.iterator().hasNext());
    }

    @Test
    @Transactional

    public void testFindAllPersonByIndexOnAnnotatedFieldWithAtIndexed() {
        Person person = persistedPerson(NAME_VALUE, 35);
        person.setNickname("Mike");
        final Person found = this.personRepository.findByPropertyValue("nickname", "Mike");
        assertEquals(person, found);
    }

    @Test
    @Transactional
    public void testNodeIsIndexed() {
        Node node = neo4jTemplate.createNode();
        node.setProperty(NAME, NAME_VALUE);
        Index<Node> nodeIndex = neo4jTemplate.getGraphDatabaseService().index().forNodes("node");
        nodeIndex.add(node, NAME, NAME_VALUE);
        Assert.assertEquals("indexed node found", node, nodeIndex.get(NAME, NAME_VALUE).next());
    }

    @Test
    @Transactional
    public void testNodeCanbBeIndexedTwice() {
        final Person p = persistedPerson(NAME_VALUE2, 30);
        Assert.assertEquals(p, personRepository.findByPropertyValue(NAME_INDEX, "name", NAME_VALUE2));
        p.setName(NAME_VALUE);
        Assert.assertEquals(p,  personRepository.findByPropertyValue(NAME_INDEX, "name", NAME_VALUE));
        p.setName(NAME_VALUE2);
        Assert.assertEquals(p,  personRepository.findByPropertyValue(NAME_INDEX, "name", NAME_VALUE2));
    }
    @Test
    public void testNodeCanbBeIndexedTwiceInDifferentTransactions() {
        Transaction tx = null;
        final Person p;
        try {
            tx = neo4jTemplate.getGraphDatabase().beginTx();
            p = persistedPerson(NAME_VALUE2, 30);
            tx.success();
        } finally {
            if (tx != null) tx.close();
        }
        Assert.assertEquals(p, personRepository.findByPropertyValue(NAME_INDEX, "name", NAME_VALUE2));
        try {
            tx = neo4jTemplate.getGraphDatabase().beginTx();
            p.setName(NAME_VALUE);
            tx.success();
        } finally {
            tx.close();
        }
        Assert.assertEquals(p,  personRepository.findByPropertyValue(NAME_INDEX, "name", NAME_VALUE));
        try {
            tx = neo4jTemplate.getGraphDatabase().beginTx();
            p.setName(NAME_VALUE2);
            tx.success();
        } finally {
            tx.close();
        }
        Assert.assertEquals(p,  personRepository.findByPropertyValue(NAME_INDEX, "name", NAME_VALUE2));
    }

    @Test
    @Transactional
    public void testRelationshipIsIndexed() {
        Node node = neo4jTemplate.createNode();
        Node node2 = neo4jTemplate.createNode();
        Relationship indexedRelationship = node.createRelationshipTo(node2, DynamicRelationshipType.withName("relatesTo"));
        indexedRelationship.setProperty(NAME, NAME_VALUE);
        Index<Relationship> relationshipIndex = neo4jTemplate.getGraphDatabaseService().index().forRelationships("relationship");
        relationshipIndex.add(indexedRelationship, NAME, NAME_VALUE);
        Assert.assertEquals("indexed relationship found", indexedRelationship, relationshipIndex.get(NAME, NAME_VALUE).next());
    }
    
    @Test
    @Transactional
    public void testUpdateBooleanPropertyIsReflectedInIndex() {
        Group group = persist(new Group());
        group.setAdmin(true);
        assertEquals(1,IteratorUtil.asCollection(groupRepository.findAllByPropertyValue("admin",true)).size());
        group.setAdmin(false);
        assertEquals(0,IteratorUtil.asCollection(groupRepository.findAllByPropertyValue("admin",true)).size());
    }
}
