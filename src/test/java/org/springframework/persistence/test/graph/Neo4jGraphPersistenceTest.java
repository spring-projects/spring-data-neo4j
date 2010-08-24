package org.springframework.persistence.test.graph;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.persistence.graph.neo4j.Finder;
import org.springframework.persistence.graph.neo4j.FinderFactory;
import org.springframework.persistence.graph.neo4j.Neo4jHelper;
import org.springframework.persistence.graph.neo4j.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;
import org.springframework.persistence.test.Friendship;
import org.springframework.persistence.test.Group;
import org.springframework.persistence.test.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class Neo4jGraphPersistenceTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private EntityInstantiator<NodeBacked,Node> graphEntityInstantiator;

	@Autowired
	protected GraphDatabaseService graphDatabaseService;
	
	@Autowired
	private FinderFactory finderFactory;
	
	@Test
	public void testStuffWasAutowired() {
        Assert.assertNotNull( graphDatabaseService );
        Assert.assertNotNull( graphEntityInstantiator );
	}
	
	@Before
	@Transactional
	public void cleanDb() {
		Neo4jHelper.cleanDb(graphDatabaseService);
	}

	@Test
	@Transactional
	public void testUserConstructor() {
		Person p = new Person("Rod", 39);
		Assert.assertEquals(p.getName(), p.getUnderlyingNode().getProperty("Person.name"));
		Assert.assertEquals(p.getAge(), p.getUnderlyingNode().getProperty("Person.age"));
		Person found = graphEntityInstantiator.createEntityFromState(graphDatabaseService.getNodeById(p.getId()), Person.class);
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
	
	@Test(expected = InvalidDataAccessResourceUsageException.class)
	public void testCreateOutsideTransaction() {
		Person p = new Person("Michael", 35);
	}
	
	@Test(expected = InvalidDataAccessResourceUsageException.class)
	public void testSetPropertyOutsideTransaction() {
		Transaction tx = graphDatabaseService.beginTx();
		Person p = null;
		try {
			p = new Person("Michael", 35);
			tx.success();
		} finally {
			tx.finish();
		}
		p.setAge(25);
	}
	
	@Test(expected = InvalidDataAccessResourceUsageException.class)
	public void testCreateRelationshipOutsideTransaction() {
		Transaction tx = graphDatabaseService.beginTx();
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
	}
	
	@Test
	public void testGetPropertyOutsideTransaction() {
		Transaction tx = graphDatabaseService.beginTx();
		Person p = null;
		try {
			p = new Person("Michael", 35);
			tx.success();
		} finally {
			tx.finish();
		}
		Assert.assertEquals("Wrong age.", (int)35, (int)p.getAge());
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
		Person pById = finder.findById(p.getId());
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
}
