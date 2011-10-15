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
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.aspects.Friendship;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.PersonRepository;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml"})
public class DynamicPropertiesTest extends EntityTestBase {
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
        persist(p);
        assertEquals(3, IteratorUtil.count(p.getPersonalProperties().getPropertyKeys()));
        assertProperties(nodeFor(p));
        p.setProperty("s", "String two");
        persist(p);
        assertEquals("String two", nodeFor(p).getProperty("personalProperties-s"));
    }

    @Test
    public void testSetNullValue() {
        final Person james = Person.persistedPerson("James", 35);
        james.setPersonalProperties(null);
        assertEquals("empty properties after setting to null", true, james.getPersonalProperties().asMap().isEmpty());
    }

    Person createTestPerson() {
        Person p = persistedPerson("James", 36);
        p.setProperty("s", "String");
        p.setProperty("x", 100);
        p.setProperty("pi", 3.1415);
        return persist(p);
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
        assertEquals(3.1415, ((Double) p2.getPersonalProperties().getProperty("pi")).doubleValue(), 0.000000001);
    }

    @Test
    @Transactional
    public void testRemoveProperty() {
        Person p = createTestPerson();

        DynamicProperties props = p.getPersonalProperties();
        props.removeProperty("s");
        persist(p);
        Node node = nodeFor(p);
        assertEquals(2, IteratorUtil.count(p.getPersonalProperties().getPropertyKeys()));
        assertFalse(node.hasProperty("personalProperties-s"));
        assertEquals(100, node.getProperty("personalProperties-x"));
        assertEquals(3.1415, ((Double) node.getProperty("personalProperties-pi")).doubleValue(), 0.000000001);
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
        persist(p);
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
        assertEquals(3.1415, ((Double) propertyMap.get("pi")).doubleValue(), 0.000000001);
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

        Relationship rel = getNodeState(john).getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);

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

        Relationship rel = getNodeState(john).getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
        assertProperties(rel, "Friendship.");
        persist(john);

        props.removeProperty("s");
        rel = getNodeState(john).getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);

        final String prefix = "Friendship.";
        assertEquals(100, rel.getProperty(prefix + "personalProperties-x"));
        assertEquals(3.1415, ((Double) rel.getProperty(prefix + "personalProperties-pi")).doubleValue(), 0.000000001);
        assertFalse(rel.hasProperty(prefix + "personalProperties-s"));
    }

    private static void assertProperties(PropertyContainer container) {
        assertProperties(container, "");
    }

    private static void assertProperties(PropertyContainer container, String prefix) {
        assertEquals(100, container.getProperty(prefix + "personalProperties-x"));
        assertEquals(3.1415, ((Double) container.getProperty(prefix + "personalProperties-pi")).doubleValue(), 0.000000001);
        assertEquals("String", container.getProperty(prefix + "personalProperties-s"));
    }

    private Node nodeFor(Person person) {
        return getNodeState(person);
    }

}
