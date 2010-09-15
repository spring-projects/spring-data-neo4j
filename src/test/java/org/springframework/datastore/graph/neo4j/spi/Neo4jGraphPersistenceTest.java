package org.springframework.datastore.graph.neo4j.spi;

import org.junit.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.index.IndexService;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.neo4j.Friendship;
import org.springframework.datastore.graph.neo4j.Group;
import org.springframework.datastore.graph.neo4j.Person;
import org.springframework.datastore.graph.neo4j.Personality;
import org.springframework.datastore.graph.neo4j.finder.Finder;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.datastore.graph.neo4j.spi.node.Neo4jHelper;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

//@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/datastore/graph/neo4j/spi/Neo4jGraphPersistenceTest-context.xml"})
public class Neo4jGraphPersistenceTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private FinderFactory finderFactory;

    @Autowired
    private IndexService indexService;

    @Test
	public void testStuffWasAutowired() {
        Assert.assertNotNull(graphDatabaseContext);
	}
	
	@Before
	@Transactional
	public void cleanDb() {
		Neo4jHelper.cleanDb(graphDatabaseContext);
    }

	@Test
	@Transactional
	public void testUserConstructor() {
		Person p = new Person("Rod", 39);
		Assert.assertEquals(p.getName(), p.getUnderlyingNode().getProperty("Person.name"));
		Assert.assertEquals(p.getAge(), p.getUnderlyingNode().getProperty("Person.age"));
		Person found = graphDatabaseContext.createEntityFromState(graphDatabaseContext.getNodeById(p.getNodeId()), Person.class);
		Assert.assertEquals("Rod", found.getUnderlyingNode().getProperty("Person.name"));
		Assert.assertEquals(39, found.getUnderlyingNode().getProperty("Person.age"));
	}
	
	@Test
	@Transactional
	public void testSetProperties() {
		Person p = new Person("Foo", 2);
		p.setName("Michael");
		p.setAge(35);
		p.setHeight((short)182);
		Assert.assertEquals("Michael", p.getUnderlyingNode().getProperty("Person.name"));
		Assert.assertEquals(35, p.getUnderlyingNode().getProperty("Person.age"));
		Assert.assertEquals((short)182, p.getUnderlyingNode().getProperty("Person.height"));
		Assert.assertEquals((short)182, (short)p.getHeight());
	}
	@Test
	@Transactional
	public void testSetShortProperty() {
		Group group = new Group();
		group.setName("developers");
		Assert.assertEquals("developers", group.getUnderlyingNode().getProperty("name"));
	}

	@Test
	@Transactional
	public void testCreateRelationshipWithoutAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina",36);
		p.setSpouse(spouse);
		Node spouseNode=p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING).getEndNode();
		Assert.assertEquals(spouse.getUnderlyingNode(), spouseNode);
		Assert.assertEquals(spouse, p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testCreateRelationshipWithAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person mother = new Person("Gabi",60);
		p.setMother(mother);
		Node motherNode = p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("mother"), Direction.OUTGOING).getEndNode();
		Assert.assertEquals(mother.getUnderlyingNode(), motherNode);
		Assert.assertEquals(mother, p.getMother());
	}

	@Test
	@Transactional
	public void testDeleteRelationship() {
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina", 36);
		p.setSpouse(spouse);
		p.setSpouse(null);
		Assert.assertNull(p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING));
		Assert.assertNull(p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testDeletePreviousRelationshipOnNewRelationship() {
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina", 36);
		Person friend = new Person("Helga", 34);
		p.setSpouse(spouse);
		p.setSpouse(friend);
		Assert.assertEquals(friend.getUnderlyingNode(), p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING).getEndNode());
		Assert.assertEquals(friend, p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testCreateIncomingRelationshipWithAnnotationOnSet() {
		Person p = new Person("David", 25);
		Person boss = new Person("Emil", 32);
		p.setBoss(boss);
		Assert.assertEquals(boss.getUnderlyingNode(), p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("boss"), Direction.INCOMING).getStartNode());
		Assert.assertEquals(boss, p.getBoss());
	}
	
	// @Test(expected = InvalidDataAccessResourceUsageException.class)
	public void testCreateOutsideTransaction() {
		Person p = new Person("Michael", 35);
        Assert.assertEquals(35,p.getAge());
	}
	
	// @Test(expected = InvalidDataAccessResourceUsageException.class)
    @Test
	public void testSetPropertyOutsideTransaction() {
		Transaction tx = graphDatabaseContext.beginTx();
		Person p = null;
		try {
			p = new Person("Michael", 35);
			tx.success();
		} finally {
			tx.finish();
		}
		p.setAge(25);
        Assert.assertEquals(25,p.getAge());
        tx = graphDatabaseContext.beginTx();
        try {
            Assert.assertEquals(25,p.getAge());
            p.setAge(20);
            tx.success();
        } finally {
            tx.finish();
        }
        Assert.assertEquals(20,p.getAge());
	}
	
    @Test
	public void testCreateRelationshipOutsideTransaction() {
		Transaction tx = graphDatabaseContext.beginTx();
		Person p = null;
		Person spouse = null;
		try {
			p = new Person("Michael", 35);
			spouse = new Person("Tina", 36);
			tx.success();
		} finally {
			tx.finish();
		}
		p.setSpouse(spouse);
        Assert.assertEquals(spouse,p.getSpouse());
        Person spouse2;
        tx = graphDatabaseContext.beginTx();
        try {
            Assert.assertEquals(spouse,p.getSpouse());
            spouse2 = new Person("Rana", 5);
            p.setSpouse(spouse2);
            tx.success();
        } finally {
            tx.finish();
        }
        Assert.assertEquals(spouse2,p.getSpouse());
	}
	
	@Test
	public void testGetPropertyOutsideTransaction() {
		Transaction tx = graphDatabaseContext.beginTx();
		Person p = null;
		try {
			p = new Person("Michael", 35);
			tx.success();
		} finally {
			tx.finish();
		}
		Assert.assertEquals("Wrong age.", (int)35, (int)p.getAge());
	}

    @Test
	public void testFindOutsideTransaction() {
        final Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
        Assert.assertEquals(false,finder.findAll().iterator().hasNext());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	@Transactional
	public void testCircularRelationship() {
		Person p = new Person("Michael", 35);
		p.setSpouse(p);
	}

	@Test
	@Transactional
	public void testSetOneToManyRelationship() {
		Person michael = new Person("Michael", 35);
		Person david = new Person("David", 25);
		Group group = new Group();
		Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
		group.setPersons(persons);
		Relationship michaelRel = michael.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.INCOMING);
		Relationship davidRel = david.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.INCOMING);
		Assert.assertEquals(group.getUnderlyingNode(), michaelRel.getStartNode());
		Assert.assertEquals(group.getUnderlyingNode(), davidRel.getStartNode());
	}

	@Test
	@Transactional
	public void testGetOneToManyRelationship() {
		Person michael = new Person("Michael", 35);
		Person david = new Person("David", 25);
		Group group = new Group();
		Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
		group.setPersons(persons);
		Collection<Person> personsFromGet = group.getPersons();
		Assert.assertEquals(persons, personsFromGet);
		Assert.assertTrue(Set.class.isAssignableFrom(personsFromGet.getClass()));
	}

	@Test
	@Transactional
	public void testAddToOneToManyRelationship() {
		Person michael = new Person("Michael", 35);
		Person david = new Person("David", 25);
		Group group = new Group();
		group.setPersons(new HashSet<Person>());
		group.getPersons().add(michael);
		group.getPersons().add(david);
		Collection<Person> personsFromGet = group.getPersons();
		Assert.assertEquals(new HashSet<Person>(Arrays.asList(david,michael)), personsFromGet);
		Assert.assertTrue(Set.class.isAssignableFrom(personsFromGet.getClass()));
	}
	
	@Test
	@Transactional
	public void testRemoveFromOneToManyRelationship() {
		Person michael = new Person("Michael", 35);
		Person david = new Person("David", 25);
		Group group = new Group();
		group.setPersons(new HashSet<Person>(Arrays.asList(michael, david)));
		group.getPersons().remove(david);
		Assert.assertEquals(Collections.singleton(michael), group.getPersons());
	}
	
	@Test
	@Transactional
	public void testFinderFindAll() {
		Person p1 = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
		Iterable<Person> allPersons = finder.findAll();
		Assert.assertEquals(new HashSet<Person>(Arrays.asList(p1, p2)), IteratorUtil.addToCollection(allPersons.iterator(), new HashSet<Person>()));
	}
	
	@Test
	@Transactional
	public void testFinderFindById() {
		Person p = new Person("Michael", 35);
		Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
		Person pById = finder.findById(p.getNodeId());
		Assert.assertEquals(p, pById);
	}
	
	@Test
	@Transactional
	public void testFinderFindByIdNonexistent() {
		Person p = new Person("Michael", 35);
		Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
		Person p2 = finder.findById(589736218);
		Assert.assertNull(p2);
	}
	
	@Test
	@Transactional
	public void testFinderCount() {
		Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
		Assert.assertEquals(0, finder.count());
		Person p = new Person("Michael", 35);
		Assert.assertEquals(1, finder.count());
	}
	
	@Test
	@Transactional
	public void testRelationshipCreate() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
		Relationship rel = p.getUnderlyingNode().getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
		Assert.assertEquals(f.getUnderlyingRelationship(), rel);
		Assert.assertEquals(p2.getUnderlyingNode(), rel.getEndNode());
	}
	
	@Test
	@Transactional
	public void testRelationshipSetProperty() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
		f.setYears(1);
		Assert.assertEquals(1, f.getUnderlyingRelationship().getProperty("Friendship.years"));
	}
	
	@Test
	@Transactional
	public void testRelationshipGetProperty() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
		f.getUnderlyingRelationship().setProperty("Friendship.years", 1);
		Assert.assertEquals(1, f.getYears());
	}
	
	@Test
	@Transactional
	public void testRelationshipGetStartNodeAndEndNode() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
		Assert.assertEquals(p, f.getPerson1());
		Assert.assertEquals(p2, f.getPerson2());
	}
	
	@Test
	@Transactional
	public void testGetRelationshipToReturnsRelationship() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
        Assert.assertEquals(f,p.getRelationshipTo(p2,Friendship.class, "knows"));
	}

	@Test
	@Transactional
	public void testRelationshipGetEntities() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Person p3 = new Person("Emil", 32);
		Friendship f2 = p.knows(p2);
		Friendship f3 = p.knows(p3);
		Assert.assertEquals(new HashSet<Friendship>(Arrays.asList(f2, f3)), IteratorUtil.addToCollection(p.getFriendships().iterator(), new HashSet<Friendship>()));
	}
	
	@Test(expected = InvalidDataAccessApiUsageException.class)
	@Transactional
	public void testRelationshipSetEntitiesShouldThrowException() {
		Person p = new Person("Michael", 35);
		p.setFriendships(new HashSet<Friendship>());
	}
	
	@Test
	@Transactional
	public void testOneToManyReadOnly() {
		Person michael = new Person("Michael", 35);
		Person david = new Person("David", 25);
		Group group = new Group();
		Set<Person> persons = new HashSet<Person>(Arrays.asList(michael, david));
		group.setPersons(persons);
		Assert.assertEquals(persons, IteratorUtil.addToCollection(group.getReadOnlyPersons().iterator(), new HashSet<Person>()));
	}
	
	@Test(expected = InvalidDataAccessApiUsageException.class)
	@Transactional
	public void testOneToManyReadOnlyShouldThrowExceptionOnSet() {
		Group group = new Group();
		group.setReadOnlyPersons(new HashSet<Person>());
	}
	
	@Test
	@Transactional
	public void testFindGroupByIndex() {
		Group group = new Group();
        group.setName("test");
        final Finder<Group> finder = finderFactory.getFinderForClass(Group.class);
        final Group found = finder.findByPropertyValue("name", "test");
        Assert.assertEquals(group,found);
	}

    @Test
	@Transactional
	public void testFindAllGroupsByIndex() {
		Group group = new Group();
        group.setName("test");
		Group group2 = new Group();
        group2.setName("test");
        final Finder<Group> finder = finderFactory.getFinderForClass(Group.class);
        final Iterable<Group> found = finder.findAllByPropertyValue("name", "test");
        final Collection<Group> result = IteratorUtil.addToCollection(found.iterator(), new HashSet<Group>());
        Assert.assertEquals(new HashSet<Group>(Arrays.asList(group,group2)), result);
	}
    
    @Test
	@Transactional
	public void testFindAllPersonByIndexOnAnnotatedField() {
		Person person = new Person("Michael",35);
        final Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
        final Person found = finder.findByPropertyValue("Person.name", "Michael");
        Assert.assertEquals(person, found);
	}
    
	@Test
	@Transactional
	public void testTraverseFromGroupToPeople() {
        Person p = new Person("Michael", 35);
		Group group = new Group();
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = new TraversalDescriptionImpl().relationships(DynamicRelationshipType.withName("persons")).filter(Traversal.returnAllButStartNode());
        Iterable<Person> people = (Iterable<Person>) group.find(Person.class, traversalDescription);
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        Assert.assertEquals(Collections.singleton(p),found);
	}

	@Test
	@Transactional
	public void testTraverseFieldFromGroupToPeople() {
        Person p = new Person("Michael", 35);
		Group group = new Group();
        group.setName("dev");
        group.addPerson(p);
        Iterable<Person> people = group.getPeople();
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        Assert.assertEquals(Collections.singleton(p),found);
	}

	@Test
	@Transactional
	public void testTraverseFromGroupToPeopleWithFinder() {
        final Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
        Person p = new Person("Michael", 35);
		Group group = new Group();
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = new TraversalDescriptionImpl().relationships(DynamicRelationshipType.withName("persons")).filter(Traversal.returnAllButStartNode());
        Iterable<Person> people = finder.findAllByTraversal(group, traversalDescription);
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        Assert.assertEquals(Collections.singleton(p),found);
	}
	
	@Test
	@Transactional
	public void testSetPropertyEnum() {
		Person p = new Person("Michael", 35);
		p.setPersonality(Personality.EXTROVERT);
		Assert.assertEquals("Wrong enum serialization.", "EXTROVERT", p.getUnderlyingNode().getProperty("Person.personality"));
	}
	
	@Test
	@Transactional
	public void testGetPropertyEnum() {
		Person p = new Person("Michael", 35);
		p.getUnderlyingNode().setProperty("Person.personality", "EXTROVERT");
		Assert.assertEquals("Did not deserialize property value properly.", Personality.EXTROVERT, p.getPersonality());
	}
	
	@Test(expected = NotFoundException.class)
	@Transactional
	public void testSetTransientPropertyFieldNotManaged() {
		Person p = new Person("Michael", 35);
		p.setThought("food");
		p.getUnderlyingNode().getProperty("Person.thought");
	}
	
	@Test
	@Transactional
	public void testGetTransientPropertyFieldNotManaged() {
		Person p = new Person("Michael", 35);
		p.setThought("food");
		p.getUnderlyingNode().setProperty("Person.thought", "sleep");
		Assert.assertEquals("Should not have read transient value from graph.", "food", p.getThought());
	}

	@Test
	@Transactional
	public void testRelationshipSetPropertyDate() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.setFirstMeetingDate(new Date(3));
		Assert.assertEquals("Date not serialized properly.", "3", f.getUnderlyingRelationship().getProperty("Friendship.firstMeetingDate"));
	}

	@Test
	@Transactional
	public void testRelationshipGetPropertyDate() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.getUnderlyingRelationship().setProperty("Friendship.firstMeetingDate", "3");
		Assert.assertEquals("Date not deserialized properly.", new Date(3), f.getFirstMeetingDate());
	}

	@Test(expected = NotFoundException.class)
	@Transactional
	public void testRelationshipSetTransientPropertyFieldNotManaged() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.setLatestLocation("Menlo Park");
		f.getUnderlyingRelationship().getProperty("Friendship.latestLocation");
	}

	@Test
	@Transactional
	public void testRelationshipGetTransientPropertyFieldNotManaged() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.setLatestLocation("Menlo Park");
		f.getUnderlyingRelationship().setProperty("Friendship.latestLocation", "Palo Alto");
		Assert.assertEquals("Should not have read transient value from graph.", "Menlo Park", f.getLatestLocation());
	}
}
