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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.neo4j.*;
import org.springframework.data.graph.neo4j.annotation.Indexed;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.repository.GraphRepository;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class IndexTest {

    private static final String NAME = "name";
    private static final String NAME_VALUE = "aName";
    private static final String NAME_VALUE2 = "aSecondName";
    private static final String NAME_VALUE3 = "aThirdName";
    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Autowired
    private DirectGraphRepositoryFactory graphRepositoryFactory;
    protected GraphRepository<Group> groupFinder;
    protected GraphRepository<Person> personFinder;
    @Autowired protected PersonRepository personRepository;
    @Autowired protected GroupRepository groupRepository;

    @Before
    public void setUp() throws Exception {
        groupFinder = graphRepositoryFactory.createNodeEntityRepository(Group.class);
        personFinder = graphRepositoryFactory.createNodeEntityRepository(Person.class);
    }

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testCanIndexIntFieldsOnRelationshipEntities() {
        Person p = persistedPerson(NAME_VALUE, 35);
        Person p2 = persistedPerson(NAME_VALUE2, 25);
        Friendship friendship = p.knows(p2);
        friendship.setYears(1);
        GraphRepository<Friendship> friendshipFinder = graphRepositoryFactory.createRelationshipEntityRepository(Friendship.class);
        assertEquals(friendship, friendshipFinder.findByPropertyValue("Friendship.years", 1));
    }

    @Test
    @Transactional
    public void testGetRelationshipFromLookedUpNode() {
        Person me = persistedPerson(NAME_VALUE, 35);
        Person spouse = persistedPerson(NAME_VALUE3, 36);
        me.setSpouse(spouse);
        final Person foundMe = this.personRepository.findByPropertyValue(Person.NAME_INDEX, "Person.name", NAME_VALUE);
        assertEquals(spouse, foundMe.getSpouse());
    }

    @Test
    //@Transactional
    //@Ignore("remove property from index not workin")
    public void testRemovePropertyFromIndex() {
        Transaction tx = graphDatabaseContext.beginTx();
        try {
            Group group = new Group().persist();
            group.setName(NAME_VALUE);
            getGroupIndex().remove(group.getPersistentState(), NAME);
            tx.success();
        } finally {
            tx.finish();
        }
        final Group found = groupFinder.findByPropertyValue( NAME, NAME_VALUE);
        assertNull("Group.name removed from index", found);
    }

    @Test
    //@Transactional
    //@Ignore("remove property from index not workin")
    public void testRemoveNodeFromIndex() {
        Transaction tx = graphDatabaseContext.beginTx();
        try {
            Group group = new Group().persist();
            group.setName(NAME_VALUE);
            getGroupIndex().remove(group.getPersistentState());
            tx.success();
        } finally {
            tx.finish();
        }
        final Group found = groupFinder.findByPropertyValue( NAME, NAME_VALUE);
        assertNull("Group.name removed from index", found);
    }

    private Index<Node> getGroupIndex() {
        return graphDatabaseContext.getIndex(Group.class);
    }

    @Test
    @Transactional
    public void testFindGroupByIndex() {
        Group group = new Group().persist();
        group.setName(NAME_VALUE);
        final Group found = groupFinder.findByPropertyValue(NAME, NAME_VALUE);
        assertEquals(group, found);
    }

    @Test
    @Transactional
    public void testFindGroupByAlternativeFieldNameIndex() {
        Group group = new Group().persist();
        group.setOtherName(NAME_VALUE);
        final Group found = groupFinder.findByPropertyValue(Group.OTHER_NAME_INDEX, NAME_VALUE);
        assertEquals(group, found);
    }

    @NodeEntity
    static class InvalidIndexed {

        @Indexed(fulltext = true)
        String fulltextNoIndexName;

        @Indexed(fulltext = true, indexName = "InvalidIndexed")
        String fullTextDefaultIndexName;

        public String getFulltextNoIndexName() {
            return fulltextNoIndexName;
        }

        public void setFulltextNoIndexName(String fulltextNoIndexName) {
            this.fulltextNoIndexName = fulltextNoIndexName;
        }

        public String getFullTextDefaultIndexName() {
            return fullTextDefaultIndexName;
        }

        public void setFullTextDefaultIndexName(String fullTextDefaultIndexName) {
            this.fullTextDefaultIndexName = fullTextDefaultIndexName;
        }
    }


    @Test(expected = IllegalStateException.class)
    @Transactional
    public void indexAccessWithFullAndNoIndexNameShouldFail() {
        InvalidIndexed invalidIndexed = new InvalidIndexed().persist();
        invalidIndexed.setFulltextNoIndexName(NAME_VALUE);
    }

    @Test(expected = IllegalStateException.class)
    @Transactional
    public void indexAccessWithFullAndDefaultIndexNameShouldFail() {
        InvalidIndexed invalidIndexed = new InvalidIndexed().persist();
        invalidIndexed.setFullTextDefaultIndexName(NAME_VALUE);
    }


    @Test
    @Transactional
    public void testDontFindGroupByNonIndexedFieldWithAnnotation() {
        Group group = new Group().persist();
        group.setUnindexedName("value-unindexedName");
        final Group found = groupFinder.findByPropertyValue("unindexedName", "value-unindexedName");
        assertNull(found);
    }

    @Test
    @Transactional
    public void testDontFindGroupByNonIndexedField() {
        Group group = new Group().persist();
        group.setUnindexedName2("value-unindexedName2");
        final Group found = groupFinder.findByPropertyValue( "unindexedName2", "value-unindexedName2");
        assertNull(found);
    }

    @Test
    @Transactional
    public void testFindAllGroupsByIndex() {
        Group group = new Group().persist();
        group.setName(NAME_VALUE);
        Group group2 = new Group().persist();
        group2.setName(NAME_VALUE);
        final Iterable<Group> found = groupFinder.findAllByPropertyValue(NAME, NAME_VALUE);
        final Collection<Group> result = IteratorUtil.addToCollection(found.iterator(), new HashSet<Group>());
        assertEquals(new HashSet<Group>(Arrays.asList(group, group2)), result);
    }

    @Test
    @Transactional
    public void shouldFindGroupyByQueryString() {
        Group group = new Group().persist();
        group.setFullTextName("queryableName");
        final Iterable<Group> found = groupRepository.findAllByQuery(Group.SEARCH_GROUPS_INDEX, "fullTextName", "queryable*");
        final Collection<Group> result = IteratorUtil.addToCollection(found.iterator(), new HashSet<Group>());
        assertEquals(new HashSet<Group>(Arrays.asList(group)), result);
    }

    @Test
    @Transactional
    public void testFindAllPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final Person found = personRepository.findByPropertyValue(Person.NAME_INDEX, "Person.name", NAME_VALUE);
        assertEquals(person, found);
    }

    @Test
    public void findsPersonByIndexOnAnnotatedIntFieldInSeparateTransactions() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final Person found = personFinder.findByPropertyValue("Person.age", 35);
        assertEquals("person found inside range", person, found);
    }

    @Test
    @Transactional
    public void testRangeQueryPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final Person found = personFinder.findAllByRange("Person.age", 10, 40).iterator().next();
        assertEquals("person found inside range", person, found);
    }

    @Test
    @Transactional
    public void testOutsideRangeQueryPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        Iterable<Person> emptyResult = personFinder.findAllByRange("Person.age", 0, 34);
        assertFalse("nothing found outside range", emptyResult.iterator().hasNext());
    }

    @Test
    @Transactional

    public void testFindAllPersonByIndexOnAnnotatedFieldWithAtIndexed() {
        Person person = persistedPerson(NAME_VALUE, 35);
        person.setNickname("Mike");
        final Person found = personFinder.findByPropertyValue( "Person.nickname", "Mike");
        assertEquals(person, found);
    }

    @Test
    @Transactional
    public void testNodeIsIndexed() {
        Node node = graphDatabaseContext.createNode();
        node.setProperty(NAME, NAME_VALUE);
        Index<Node> nodeIndex = graphDatabaseContext.getGraphDatabaseService().index().forNodes("node");
        nodeIndex.add(node, NAME, NAME_VALUE);
        Assert.assertEquals("indexed node found", node, nodeIndex.get(NAME, NAME_VALUE).next());
    }

    @Test
    @Transactional
    public void testRelationshipIsIndexed() {
        Node node = graphDatabaseContext.createNode();
        Node node2 = graphDatabaseContext.createNode();
        Relationship indexedRelationship = node.createRelationshipTo(node2, DynamicRelationshipType.withName("relatesTo"));
        indexedRelationship.setProperty(NAME, NAME_VALUE);
        Index<Relationship> relationshipIndex = graphDatabaseContext.getGraphDatabaseService().index().forRelationships("relationship");
        relationshipIndex.add(indexedRelationship, NAME, NAME_VALUE);
        Assert.assertEquals("indexed relationship found", indexedRelationship, relationshipIndex.get(NAME, NAME_VALUE).next());
    }
}
