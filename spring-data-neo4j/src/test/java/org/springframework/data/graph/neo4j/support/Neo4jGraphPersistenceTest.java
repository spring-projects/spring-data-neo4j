package org.springframework.data.graph.neo4j.support;

import org.junit.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.neo4j.*;
import org.springframework.data.graph.neo4j.finder.Finder;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.data.graph.neo4j.Car;
import org.springframework.data.graph.neo4j.Friendship;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.Personality;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class Neo4jGraphPersistenceTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private FinderFactory finderFactory;

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
		assertEquals(p.getName(), p.getUnderlyingState().getProperty("Person.name"));
		assertEquals(p.getAge(), p.getUnderlyingState().getProperty("Person.age"));
		Person found = graphDatabaseContext.createEntityFromState(graphDatabaseContext.getNodeById(p.getNodeId()), Person.class);
		assertEquals("Rod", found.getUnderlyingState().getProperty("Person.name"));
		assertEquals(39, found.getUnderlyingState().getProperty("Person.age"));
	}
	
	@Test
	@Transactional
	public void testSetProperties() {
		Person p = new Person("Foo", 2);
		p.setName("Michael");
		p.setAge(35);
		p.setHeight((short)182);
		assertEquals("Michael", p.getUnderlyingState().getProperty("Person.name"));
		assertEquals(35, p.getUnderlyingState().getProperty("Person.age"));
		assertEquals((short)182, p.getUnderlyingState().getProperty("Person.height"));
		assertEquals((short)182, (short)p.getHeight());
	}
	@Test
	@Transactional
	public void testSetShortProperty() {
		Group group = new Group();
		group.setName("developers");
		assertEquals("developers", group.getUnderlyingState().getProperty("name"));
	}

	@Test
	@Transactional
	public void testCreateRelationshipWithoutAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina",36);
		p.setSpouse(spouse);
		Node spouseNode=p.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING).getEndNode();
		assertEquals(spouse.getUnderlyingState(), spouseNode);
		assertEquals(spouse, p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testCreateRelationshipWithAnnotationOnSet() {
		Person p = new Person("Michael", 35);
		Person mother = new Person("Gabi",60);
		p.setMother(mother);
		Node motherNode = p.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("mother"), Direction.OUTGOING).getEndNode();
		assertEquals(mother.getUnderlyingState(), motherNode);
		assertEquals(mother, p.getMother());
	}

	@Test
	@Transactional
	public void testDeleteRelationship() {
		Person p = new Person("Michael", 35);
		Person spouse = new Person("Tina", 36);
		p.setSpouse(spouse);
		p.setSpouse(null);
		Assert.assertNull(p.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING));
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
		assertEquals(friend.getUnderlyingState(), p.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("Person.spouse"), Direction.OUTGOING).getEndNode());
		assertEquals(friend, p.getSpouse());
	}
	
	@Test
	@Transactional
	public void testCreateIncomingRelationshipWithAnnotationOnSet() {
		Person p = new Person("David", 25);
		Person boss = new Person("Emil", 32);
		p.setBoss(boss);
		assertEquals(boss.getUnderlyingState(), p.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("boss"), Direction.INCOMING).getStartNode());
		assertEquals(boss, p.getBoss());
	}
	
	// @Test(expected = InvalidDataAccessResourceUsageException.class)
	public void testCreateOutsideTransaction() {
		Person p = new Person("Michael", 35);
        assertEquals(35,p.getAge());
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
        assertEquals(25,p.getAge());
        tx = graphDatabaseContext.beginTx();
        try {
            assertEquals(25,p.getAge());
            p.setAge(20);
            tx.success();
        } finally {
            tx.finish();
        }
        assertEquals(20,p.getAge());
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
        assertEquals(spouse,p.getSpouse());
        Person spouse2;
        tx = graphDatabaseContext.beginTx();
        try {
            assertEquals(spouse,p.getSpouse());
            spouse2 = new Person("Rana", 5);
            p.setSpouse(spouse2);
            tx.success();
        } finally {
            tx.finish();
        }
        assertEquals(spouse2,p.getSpouse());
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
		assertEquals("Wrong age.", (int)35, (int)p.getAge());
	}

    @Test
	public void testFindOutsideTransaction() {
        final Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
        assertEquals(false,finder.findAll().iterator().hasNext());
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
		Relationship michaelRel = michael.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.INCOMING);
		Relationship davidRel = david.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.INCOMING);
		assertEquals(group.getUnderlyingState(), michaelRel.getStartNode());
		assertEquals(group.getUnderlyingState(), davidRel.getStartNode());
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
		assertEquals(persons, personsFromGet);
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
		assertEquals(new HashSet<Person>(Arrays.asList(david,michael)), personsFromGet);
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
		assertEquals(Collections.singleton(michael), group.getPersons());
	}
	
	@Test
	@Transactional
	public void testFinderFindAll() {
		Person p1 = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
		Iterable<Person> allPersons = finder.findAll();
		assertEquals(new HashSet<Person>(Arrays.asList(p1, p2)), IteratorUtil.addToCollection(allPersons.iterator(), new HashSet<Person>()));
	}
	
	@Test
	@Transactional
	public void testFinderFindById() {
		Person p = new Person("Michael", 35);
		Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
		Person pById = finder.findById(p.getNodeId());
		assertEquals(p, pById);
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
		assertEquals(0, finder.count());
		Person p = new Person("Michael", 35);
		assertEquals(1, finder.count());
	}
	
	@Test
	@Transactional
	public void testRelationshipCreate() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
		Relationship rel = p.getUnderlyingState().getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
		assertEquals(f.getUnderlyingState(), rel);
		assertEquals(p2.getUnderlyingState(), rel.getEndNode());
	}
	
	@Test
	@Transactional
	public void testRelationshipSetProperty() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
		f.setYears(1);
		assertEquals(1, f.getUnderlyingState().getProperty("Friendship.years"));
	}
	
	@Test
	@Transactional
	public void testRelationshipGetProperty() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
		f.getUnderlyingState().setProperty("Friendship.years", 1);
		assertEquals(1, f.getYears());
	}
	
	@Test
	@Transactional
	public void testRelationshipGetStartNodeAndEndNode() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
		assertEquals(p, f.getPerson1());
		assertEquals(p2, f.getPerson2());
	}
	
	@Test
	@Transactional
	public void testGetRelationshipToReturnsRelationship() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Friendship f = p.knows(p2);
        assertEquals(f,p.getRelationshipTo(p2,Friendship.class, "knows"));
	}

	@Test
	@Transactional
	public void testGetRelationshipFromLookedUpNode() {
		Person me = new Person("Michael", 35);
		Person spouse = new Person("Tina", 36);
		me.setSpouse(spouse);
        final Finder<Person> personFinder = finderFactory.getFinderForClass(Person.class);
        final Person foundMe = personFinder.findByPropertyValue("Person.name", "Michael");
        assertEquals(spouse,foundMe.getSpouse());
	}

    @Test
	@Transactional
	public void testFindAllOnGroup() {
	    log.debug("FindAllOnGroup start");
        Group g=new Group();
        g.setName("test");
        Group g2=new Group();
        g.setName("test");
        final Finder<Group> finder = finderFactory.getFinderForClass(Group.class);
        Collection<Group> groups = IteratorUtil.addToCollection(finder.findAll().iterator(), new HashSet<Group>());
        Assert.assertEquals(2, groups.size());
	    log.debug("FindAllOnGroup done");
	}

	@Test
	@Transactional
	public void testRelationshipGetEntities() {
		Person p = new Person("Michael", 35);
		Person p2 = new Person("David", 25);
		Person p3 = new Person("Emil", 32);
		Friendship f2 = p.knows(p2);
		Friendship f3 = p.knows(p3);
		assertEquals(new HashSet<Friendship>(Arrays.asList(f2, f3)), IteratorUtil.addToCollection(p.getFriendships().iterator(), new HashSet<Friendship>()));
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
		assertEquals(persons, IteratorUtil.addToCollection(group.getReadOnlyPersons().iterator(), new HashSet<Person>()));
	}
	
	@Test(expected = InvalidDataAccessApiUsageException.class)
	@Transactional
	public void testOneToManyReadOnlyShouldThrowExceptionOnSet() {
		Group group = new Group();
		group.setReadOnlyPersons(new HashSet<Person>());
	}

    @Test
    @Transactional
    @Ignore("Missing method to remove a node/property from index")
    public void testRemovePropertyFromIndex() {
        Group group = new Group();
        group.setName("test");
        final Finder<Group> finder = finderFactory.getFinderForClass(Group.class);
        graphDatabaseContext.removeIndex("node", group.getUnderlyingState(), "name");
        final Group found = finder.findByPropertyValue("name", "test");
        assertNull("Group.name removed from index", found);
    }
	
	@Test
	@Transactional
	public void testFindGroupByIndex() {
		Group group = new Group();
        group.setName("test");
        final Finder<Group> finder = finderFactory.getFinderForClass(Group.class);
        final Group found = finder.findByPropertyValue("name", "test");
        assertEquals(group,found);
	}
	@Test
	@Transactional
	public void testDontFindGroupByNonIndexedFieldWithAnnotation() {
		Group group = new Group();
        group.setUnindexedName("value-unindexedName");
        final Finder<Group> finder = finderFactory.getFinderForClass(Group.class);
        final Group found = finder.findByPropertyValue("unindexedName", "value-unindexedName");
        assertNull(found);
	}
	@Test
	@Transactional
	public void testDontFindGroupByNonIndexedField() {
		Group group = new Group();
        group.setUnindexedName2("value-unindexedName2");
        final Finder<Group> finder = finderFactory.getFinderForClass(Group.class);
        final Group found = finder.findByPropertyValue("unindexedName2", "value-unindexedName2");
        assertNull(found);
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
        assertEquals(new HashSet<Group>(Arrays.asList(group,group2)), result);
	}
    
    @Test
	@Transactional
	public void testFindAllPersonByIndexOnAnnotatedField() {
		Person person = new Person("Michael",35);
        final Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
        final Person found = finder.findByPropertyValue("Person.name", "Michael");
	    assertEquals(person, found);
    }

	@Test
	@Transactional
	public void testFindAllPersonByIndexOnAnnotatedFieldWithAtIndexed() {
		Person person = new Person("Michael", 35);
		person.setNickname("Mike");
		final Finder<Person> finder = finderFactory.getFinderForClass(Person.class);
		final Person found = finder.findByPropertyValue("Person.nickname", "Mike");
		assertEquals(person, found);
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
        assertEquals(Collections.singleton(p),found);
	}

	@Test
	@Transactional
	@Rollback(false)
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
        assertEquals(Collections.singleton(p),found);
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
        assertEquals(Collections.singleton(p),found);
	}
	
	@Test
	@Transactional
	public void testSetPropertyEnum() {
		Person p = new Person("Michael", 35);
		p.setPersonality(Personality.EXTROVERT);
		assertEquals("Wrong enum serialization.", "EXTROVERT", p.getUnderlyingState().getProperty("Person.personality"));
	}
	
	@Test
	@Transactional
	public void testGetPropertyEnum() {
		Person p = new Person("Michael", 35);
		p.getUnderlyingState().setProperty("Person.personality", "EXTROVERT");
		assertEquals("Did not deserialize property value properly.", Personality.EXTROVERT, p.getPersonality());
	}
	
	@Test(expected = NotFoundException.class)
	@Transactional
	public void testSetTransientPropertyFieldNotManaged() {
		Person p = new Person("Michael", 35);
		p.setThought("food");
		p.getUnderlyingState().getProperty("Person.thought");
	}
	
	@Test
	@Transactional
	public void testGetTransientPropertyFieldNotManaged() {
		Person p = new Person("Michael", 35);
		p.setThought("food");
		p.getUnderlyingState().setProperty("Person.thought", "sleep");
		assertEquals("Should not have read transient value from graph.", "food", p.getThought());
	}

	@Test
	@Transactional
	@Rollback(false)
	public void testRelationshipSetPropertyDate() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.setFirstMeetingDate(new Date(3));
		assertEquals("Date not serialized properly.", "3", f.getUnderlyingState().getProperty("Friendship.firstMeetingDate"));
	}

	@Test
	@Transactional
	public void testRelationshipGetPropertyDate() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.getUnderlyingState().setProperty("Friendship.firstMeetingDate", "3");
		assertEquals("Date not deserialized properly.", new Date(3), f.getFirstMeetingDate());
	}

	@Test(expected = NotFoundException.class)
	@Transactional
	public void testRelationshipSetTransientPropertyFieldNotManaged() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.setLatestLocation("Menlo Park");
		f.getUnderlyingState().getProperty("Friendship.latestLocation");
	}

	@Test
	@Transactional
	public void testRelationshipGetTransientPropertyFieldNotManaged() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
        f.setLatestLocation("Menlo Park");
		f.getUnderlyingState().setProperty("Friendship.latestLocation", "Palo Alto");
		assertEquals("Should not have read transient value from graph.", "Menlo Park", f.getLatestLocation());
	}

	@Test
	@Transactional
	public void testEntityIdField() {
		Person p = new Person("Michael", 35);
		assertEquals("Wrong ID.", p.getUnderlyingState().getId(), p.getId());
	}

	// Would like to have this working.
	@Ignore
	@Test
	@Transactional
	public void testRelationshipIdField() {
		Person p = new Person("Michael", 35);
        Person p2 = new Person("David", 25);
        Friendship f = p.knows(p2);
//		assertEquals("Wrong ID.", f.getUnderlyingState().getId(), f.getId());
	}

	@Test
	@Transactional
	public void testInstantiateConcreteClass() {
		log.debug("testInstantiateConcreteClass");
		Person p = new Person("Michael", 35);
		Car c = new Volvo();
		p.setCar(c);
		assertEquals("Wrong concrete class.", Volvo.class, p.getCar().getClass());
	}

	@Test
	@Transactional
	public void testInstantiateConcreteClassWithFinder() {
		log.debug("testInstantiateConcreteClassWithFinder");
		new Volvo();
		Finder<Car> finder = finderFactory.getFinderForClass(Car.class);
		assertEquals("Wrong concrete class.", Volvo.class, finder.findAll().iterator().next().getClass());
	}

	@Test
	@Transactional
	public void testCountSubclasses() {
		log.debug("testCountSubclasses");
		new Volvo();
		log.debug("Created volvo");
		new Toyota();
		log.debug("Created volvo");
		assertEquals("Wrong count.", 1, finderFactory.getFinderForClass(Volvo.class).count());
		assertEquals("Wrong count.", 1, finderFactory.getFinderForClass(Toyota.class).count());
		assertEquals("Wrong count.", 2, finderFactory.getFinderForClass(Car.class).count());
	}
}
