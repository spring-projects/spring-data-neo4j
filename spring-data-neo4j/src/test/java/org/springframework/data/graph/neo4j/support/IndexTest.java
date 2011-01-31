package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Friendship;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.finder.RelationshipFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Before
	@Transactional
	public void cleanDb() {
		Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testCanIndexIntFieldsOnRelationshipEntities() {
        Person p = new Person(NAME_VALUE, 35);
        Person p2 = new Person(NAME_VALUE2, 25);
        Friendship friendship = p.knows(p2);
        friendship.setYears(1);
        RelationshipFinder<Friendship> friendshipFinder = finderFactory.createRelationshipEntityFinder(Friendship.class);
        assertEquals(friendship, friendshipFinder.findByPropertyValue(null, "Friendship.years", 1));
    }

    @Test
    @Transactional
    public void testGetRelationshipFromLookedUpNode() {
        Person me = new Person(NAME_VALUE, 35);
        Person spouse = new Person(NAME_VALUE3, 36);
        me.setSpouse(spouse);
        final NodeFinder<Person> personFinder = finderFactory.createNodeEntityFinder(Person.class);
        final Person foundMe = personFinder.findByPropertyValue(Person.NAME_INDEX, "Person.name", NAME_VALUE);
        assertEquals(spouse,foundMe.getSpouse());
    }
    @Test
    @Transactional
    @Ignore("Missing method to remove a node/property from index")
    public void testRemovePropertyFromIndex() {
        Group group = new Group();
        group.setName(NAME_VALUE);
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        graphDatabaseContext.getNodeIndex("node").remove(group.getUnderlyingState(), NAME, null);
        final Group found = finder.findByPropertyValue(null, NAME, NAME_VALUE);
        assertNull("Group.name removed from index", found);
    }

	@Test
	@Transactional
	public void testFindGroupByIndex() {
		Group group = new Group();
        group.setName(NAME_VALUE);
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Group found = finder.findByPropertyValue(null, NAME, NAME_VALUE);
        assertEquals(group,found);
	}
	@Test
	@Transactional
	public void testDontFindGroupByNonIndexedFieldWithAnnotation() {
		Group group = new Group();
        group.setUnindexedName("value-unindexedName");
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Group found = finder.findByPropertyValue(null, "unindexedName", "value-unindexedName");
        assertNull(found);
	}
	@Test
	@Transactional
	public void testDontFindGroupByNonIndexedField() {
		Group group = new Group();
        group.setUnindexedName2("value-unindexedName2");
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Group found = finder.findByPropertyValue(null, "unindexedName2", "value-unindexedName2");
        assertNull(found);
	}

    @Test
	@Transactional
	public void testFindAllGroupsByIndex() {
		Group group = new Group();
        group.setName(NAME_VALUE);
		Group group2 = new Group();
        group2.setName(NAME_VALUE);
        final NodeFinder<Group> finder = finderFactory.createNodeEntityFinder(Group.class);
        final Iterable<Group> found = finder.findAllByPropertyValue(null, NAME, NAME_VALUE);
        final Collection<Group> result = IteratorUtil.addToCollection(found.iterator(), new HashSet<Group>());
        assertEquals(new HashSet<Group>(Arrays.asList(group, group2)), result);
	}

    @Test
	@Transactional
	public void testFindAllPersonByIndexOnAnnotatedField() {
		Person person = new Person(NAME_VALUE,35);
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        final Person found = finder.findByPropertyValue( Person.NAME_INDEX, "Person.name", NAME_VALUE);
	    assertEquals(person, found);
    }

	@Test
	@Transactional
	public void testFindAllPersonByIndexOnAnnotatedFieldWithAtIndexed() {
		Person person = new Person(NAME_VALUE, 35);
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
        Index<Node> nodeIndex = graphDatabaseContext.getNodeIndex(null);
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
        Index<Relationship> relationshipIndex = graphDatabaseContext.getRelationshipIndex(null);
        relationshipIndex.add(indexedRelationship, NAME, NAME_VALUE);
        Assert.assertEquals("indexed relationship found", indexedRelationship, relationshipIndex.get(NAME, NAME_VALUE).next());
    }

}
