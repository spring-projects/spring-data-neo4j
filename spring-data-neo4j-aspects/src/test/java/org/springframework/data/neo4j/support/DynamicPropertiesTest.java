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

package org.springframework.data.neo4j.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.data.neo4j.Person.persistedPerson;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.Friendship;
import org.springframework.data.neo4j.Person;
import org.springframework.data.neo4j.PersonRepository;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:org/springframework/data/neo4j/support/Neo4jGraphPersistenceTest-context.xml"} )
public class DynamicPropertiesTest
{
    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Autowired
    private PersonRepository personRepository;
    
    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    /**
     * The dynamic properties can only be used, after the entity has been persisted and has an entity state.
     */
    @Test
	public void testCreateOutsideTransaction() {
		Person p = new Person("James", 35);
    	p.setProperty("s", "String");
    	p.setProperty("x", 100);
    	p.setProperty("pi", 3.1415);
		p.persist();
		assertEquals(3, IteratorUtil.count(p.getPersonalProperties().getPropertyKeys()));
		assertProperties(nodeFor(p));
		p.setProperty("s", "String two");
		p.persist();
		assertEquals("String two", nodeFor(p).getProperty("personalProperties-s"));	
	}
    
    @Test
    public void testSetNullValue() {
        final Person james = Person.persistedPerson("James", 35);
        james.setPersonalProperties(null);
        assertEquals("empty properties after setting to null",true,james.getPersonalProperties().asMap().isEmpty());
    }        
    Person createTestPerson() {
		Person p = persistedPerson("James", 36);
		p.setProperty("s", "String");
		p.setProperty("x", 100);
		p.setProperty("pi", 3.1415);
		return p.persist();
    }
    
    @Test
    @Transactional
    public void testProperties() {
    	Person p = createTestPerson();
    	assertEquals(3, IteratorUtil.count(p.getPersonalProperties().getPropertyKeys()));
		assertProperties(nodeFor(p));
    }
    
    @Test
    @Transactional
    public void testReload() {
    	Person p = createTestPerson();
    	Person p2 = personRepository.findOne(p.getId());
    	assertEquals(3, IteratorUtil.count(p2.getPersonalProperties().getPropertyKeys()));
    	assertEquals("String", p2.getPersonalProperties().getProperty("s"));
    	assertEquals(100, p2.getPersonalProperties().getProperty("x"));
    	assertEquals(3.1415, ((Double)p2.getPersonalProperties().getProperty("pi")).doubleValue(), 0.000000001);
    }
    
    @Test
    @Transactional
    public void testRemoveProperty() {
    	Person p = createTestPerson();
		
		DynamicProperties props = p.getPersonalProperties();
		props.removeProperty("s");
		p.persist();
		Node node = nodeFor(p);
		assertEquals(2, IteratorUtil.count(p.getPersonalProperties().getPropertyKeys()));
		assertFalse(node.hasProperty("personalProperties-s"));
		assertEquals(100, node.getProperty("personalProperties-x"));
		assertEquals(3.1415, ((Double)node.getProperty("personalProperties-pi")).doubleValue(), 0.000000001);
    }
    
    @Test
    @Transactional
    public void testFromMap() {
    	Person p = persistedPerson("James", 36);
		
    	Map<String, Object> propertyMap = new HashMap<String, Object>();
    	propertyMap.put("s", "String");
    	propertyMap.put("x", 100);
    	propertyMap.put("pi", 3.1415);
    	
    	p.setPersonalProperties(p.getPersonalProperties().createFrom(propertyMap));
		p.persist();
		assertEquals(3, IteratorUtil.count(p.getPersonalProperties().getPropertyKeys()));
		assertProperties(nodeFor(p));
    }
    
    @Test
    @Transactional
    public void testAsMap() {
    	Person p = createTestPerson();
		Map<String, Object> propertyMap = p.getPersonalProperties().asMap();
		assertEquals(3, propertyMap.size());
		assertEquals(100, propertyMap.get("x"));
		assertEquals(3.1415, ((Double)propertyMap.get("pi")).doubleValue(), 0.000000001);
		assertEquals("String", propertyMap.get("s"));	
    }

    @Test
    @Transactional
    public void testRelationshipProperties() {
    	Person james = persistedPerson("James", 36);
    	Person john = persistedPerson("John", 36);
    	Friendship f = john.knows(james);
    	DynamicProperties props = f.getPersonalProperties(); 
    	props.setProperty("s", "String");
    	props.setProperty("x", 100);
    	props.setProperty("pi", 3.1415);
    	
    	Relationship rel = john.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
    	
		assertProperties(rel, "Friendship.");
    }
    
    @Test
    @Transactional
    public void testRelationshipRemoveProperty() {
    	Person james = persistedPerson("James", 36);
    	Person john = persistedPerson("John", 36);
    	Friendship f = john.knows(james);
    	DynamicProperties props = f.getPersonalProperties(); 
    	props.setProperty("s", "String");
    	props.setProperty("x", 100);
    	props.setProperty("pi", 3.1415);
    	
    	Relationship rel = john.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
    	assertProperties(rel, "Friendship.");
    	john.persist();
    	
    	props.removeProperty("s");
    	rel = john.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
    	
    	final String prefix = "Friendship.";
		assertEquals(100, rel.getProperty(prefix + "personalProperties-x"));
		assertEquals(3.1415, ((Double)rel.getProperty(prefix + "personalProperties-pi")).doubleValue(), 0.000000001);
		assertFalse(rel.hasProperty(prefix + "personalProperties-s"));	
    }
    
    private static void assertProperties(PropertyContainer container) {
    	assertProperties(container, "");
    }
    
    private static void assertProperties(PropertyContainer container, String prefix) {
		assertEquals(100, container.getProperty(prefix + "personalProperties-x"));
		assertEquals(3.1415, ((Double)container.getProperty(prefix + "personalProperties-pi")).doubleValue(), 0.000000001);
		assertEquals("String", container.getProperty(prefix + "personalProperties-s"));	
    }
    
    private Node nodeFor(Person person) {
        return person.getPersistentState();
    }

}
