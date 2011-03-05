package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.neo4j.Friendship;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.finder.RelationshipFinder;
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
    private FinderFactory finderFactory;

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
        RelationshipFinder<Friendship> friendshipFinder = finderFactory.createRelationshipEntityFinder(Friendship.class);
        assertEquals(friendship, friendshipFinder.findByPropertyValue(null, "Friendship.years", 1));
    }

    @Test
    @Transactional
    public void testGetRelationshipFromLookedUpNode() {
        Person me = persistedPerson(NAME_VALUE, 35);
        Person spouse = persistedPerson(NAME_VALUE3, 36);
        me.setSpouse(spouse);
        final NodeFinder<Person> personFinder = finderFactory.createNodeEntityFinder(Person.class);
        final Person foundMe = personFinder.findByPropertyValue(Person.NAME_INDEX, "Person.name", NAME_VALUE);
        assertEquals(spouse, foundMe.getSpouse());
    }

    @Test
    @Transactional
    @Ignore("remove property from index not workin")
    public void testRemovePropertyFromIndex() {
        Group group = new Group().persist();
        group.setName(NAME_VALUE);
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        getGroupIndex().remove(group.getPersistentState(), NAME);
        final Group found = finder.findByPropertyValue(null, NAME, NAME_VALUE);
        assertNull("Group.name removed from index", found);
    }

    @Test
    @Transactional
    @Ignore("remove property from index not workin")
    public void testRemoveNodeFromIndex() {
        Group group = new Group().persist();
        group.setName(NAME_VALUE);
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        getGroupIndex().remove(group.getPersistentState());
        final Group found = finder.findByPropertyValue(null, NAME, NAME_VALUE);
        assertNull("Group.name removed from index", found);
    }

    private Index<Node> getGroupIndex() {
        return graphDatabaseContext.getIndex(Group.class, null);
    }

    @Test
    @Transactional
    public void testFindGroupByIndex() {
        Group group = new Group().persist();
        group.setName(NAME_VALUE);
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Group found = finder.findByPropertyValue(null, NAME, NAME_VALUE);
        assertEquals(group, found);
    }

    @Test
    @Transactional
    public void testFindGroupByAlternativeFieldNameIndex() {
        Group group = new Group().persist();
        group.setOtherName(NAME_VALUE);
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Group found = finder.findByPropertyValue(null, Group.OTHER_NAME_INDEX, NAME_VALUE);
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
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Group found = finder.findByPropertyValue(null, "unindexedName", "value-unindexedName");
        assertNull(found);
    }

    @Test
    @Transactional
    public void testDontFindGroupByNonIndexedField() {
        Group group = new Group().persist();
        group.setUnindexedName2("value-unindexedName2");
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Group found = finder.findByPropertyValue(null, "unindexedName2", "value-unindexedName2");
        assertNull(found);
    }

    @Test
    @Transactional
    public void testFindAllGroupsByIndex() {
        Group group = new Group().persist();
        group.setName(NAME_VALUE);
        Group group2 = new Group().persist();
        group2.setName(NAME_VALUE);
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Iterable<Group> found = finder.findAllByPropertyValue(null, NAME, NAME_VALUE);
        final Collection<Group> result = IteratorUtil.addToCollection(found.iterator(), new HashSet<Group>());
        assertEquals(new HashSet<Group>(Arrays.asList(group, group2)), result);
    }

    @Test
    @Transactional
    public void shouldFindGroupyByQueryString() {
        Group group = new Group().persist();
        group.setFullTextName("queryableName");
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Iterable<Group> found = finder.findAllByQuery(Group.SEARCH_GROUPS_INDEX, "fullTextName", "queryable*");
        final Collection<Group> result = IteratorUtil.addToCollection(found.iterator(), new HashSet<Group>());
        assertEquals(new HashSet<Group>(Arrays.asList(group)), result);
    }

    @Test
    @Transactional
    public void testFindAllPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        final Person found = finder.findByPropertyValue(Person.NAME_INDEX, "Person.name", NAME_VALUE);
        assertEquals(person, found);
    }

    @Test
    @Transactional
    public void testRangeQueryPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        final Person found = finder.findAllByRange(null, "Person.age", 10, 40).iterator().next();
        assertEquals("person found inside range", person, found);
    }

    @Test
    @Transactional
    public void testOutsideRangeQueryPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Iterable<Person> emptyResult = finder.findAllByRange(null, "Person.age", 0, 34);
        assertFalse("nothing found outside range", emptyResult.iterator().hasNext());
    }

    @Test
    @Transactional
    public void testFindAllPersonByIndexOnAnnotatedFieldWithAtIndexed() {
        Person person = persistedPerson(NAME_VALUE, 35);
        person.setNickname("Mike");
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        final Person found = finder.findByPropertyValue(null, "Person.nickname", "Mike");
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
