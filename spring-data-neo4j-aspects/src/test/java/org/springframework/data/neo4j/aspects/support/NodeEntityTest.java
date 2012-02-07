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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.neo4j.aspects.Attribute;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml"})

public class NodeEntityTest extends EntityTestBase {

    @Test
    @Transactional
    public void testUserConstructor() {
        Person p = persistedPerson("Rod", 39);
        assertEquals(p.getName(), getNodeState(p).getProperty("name"));
        assertEquals(p.getAge(), getNodeState(p).getProperty("age"));
        Person found = neo4jTemplate.createEntityFromState(neo4jTemplate.getNode(getNodeId(p)), Person.class, neo4jTemplate.getMappingPolicy(p));
        assertEquals("Rod", getNodeState(found).getProperty("name"));
        assertEquals(39, getNodeState(found).getProperty("age"));
    }

    @Test
    @Transactional
    public void testSetSimpleProperties() {
        String name = "Michael";
        int age = 35;
        short height = 182;

        Person p = persistedPerson("Foo", 2);
        p.setName( name );
        p.setAge( age );
        p.setHeight( height );
        assertEquals( name, getNodeState(p).getProperty("name") );
        assertEquals( age, getNodeState(p).getProperty("age"));
        assertEquals((Short)height, p.getHeight());
    }

    @Test
    public void testEntityIsStillDetachedAfterValidationException() {
        Person p = new Person("Foo", 2);
        try {
            p.setName("A");
            p.persist();
            fail("should fail to validate");
        } catch(ValidationException ve) {
            System.out.println(ve.getClass());
        }
        assertEquals("A",p.getName());
    }

    @Test
    @Transactional
    public void testArrayProperties() {
        Group g = new Group().persist();
        final String[] roleNames = {"a", "b", "c"};
        g.setRoleNames(roleNames);
        assertArrayEquals(roleNames, (String[])getNodeState(g).getProperty("roleNames"));
        assertArrayEquals(roleNames, g.getRoleNames());
    }

    @Test
    @Transactional
    public void testConvertedArrayProperties() {
        Group g = new Group().persist();
        g.setRoles(Group.Role.values());
        assertArrayEquals(new String[] {"ADMIN","USER"}, (String[])getNodeState(g).getProperty("roles"));
        assertArrayEquals(Group.Role.values(), g.getRoles());
    }
    @Test
    @Transactional
    public void testCollectionProperties() {
        Group g = new Group().persist();
        final List<String> roleNames = asList("a", "b", "c");
        g.setRoleNamesColl(roleNames);
        assertArrayEquals(roleNames.toArray(), (String[])getNodeState(g).getProperty("roleNamesColl"));
        assertEquals(roleNames, g.getRoleNamesColl());
    }

    @Test
    @Transactional
    public void testConvertedCollectionProperties() {
        Group g = new Group().persist();
        g.setRolesColl(asList(Group.Role.values()));
        assertArrayEquals(new String[] {"ADMIN","USER"}, (String[])getNodeState(g).getProperty("rolesColl"));
        assertEquals(asList(Group.Role.values()), g.getRolesColl());
    }
    @Test
    @Transactional
    public void testIterableProperties() {
        Group g = new Group().persist();
        final List<String> roleNames = asList("a", "b", "c");
        g.setRoleNamesIterable(roleNames);
        assertArrayEquals(roleNames.toArray(), (String[])getNodeState(g).getProperty("roleNamesIterable"));
        assertEquals(roleNames, g.getRoleNamesIterable());
    }

    @Test
    @Transactional
    public void testConvertedIterableProperties() {
        Group g = new Group().persist();
        g.setRolesIterable(asList(Group.Role.values()));
        assertArrayEquals(new String[] {"ADMIN","USER"}, (String[])getNodeState(g).getProperty("rolesIterable"));
        assertEquals(asList(Group.Role.values()), g.getRolesIterable());
    }
    @Test
    @Transactional
    public void testSetProperties() {
        Group g = new Group().persist();
        final Set<String> roleNames = new LinkedHashSet<String>(asList("a", "b", "c"));
        g.setRoleNamesSet(roleNames);
        assertArrayEquals(roleNames.toArray(), (String[])getNodeState(g).getProperty("roleNamesSet"));
        assertEquals(roleNames, g.getRoleNamesSet());
    }

    @Test
    @Transactional
    public void testConvertedSetProperties() {
        Group g = new Group().persist();
        final LinkedHashSet<Group.Role> roles = new LinkedHashSet<Group.Role>(asList(Group.Role.values()));
        g.setRolesSet(roles);
        assertArrayEquals(new String[] {"ADMIN","USER"}, (String[])getNodeState(g).getProperty("rolesSet"));
        assertEquals(roles, g.getRolesSet());
    }

    @Test
    @Transactional
    public void testSetShortProperty() {
        Person p = persistedPerson("Foo", 2);
        p.setHeight((short)182);
        assertEquals((Short)(short)182, p.getHeight());
        assertEquals((short)182, getNodeState(p).getProperty("height"));
    }
    @Test
    @Transactional
    public void testSetShortNameProperty() {
        Group group = persist(new Group());
        group.setName("developers");
        assertEquals("developers", getNodeState(group).getProperty("name"));
    }
    // own transaction handling because of http://wiki.neo4j.org/content/Delete_Semantics
    @Test(expected = DataRetrievalFailureException.class)
    public void testDeleteEntityFromGDC() {
        Transaction tx = neo4jTemplate.beginTx();
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        neo4jTemplate.delete(spouse);
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        Person spouseFromIndex = personRepository.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index",spouseFromIndex);
        Assert.assertNull("node deleted " + id, neo4jTemplate.getNode(id));
    }

    @Test(expected = DataRetrievalFailureException.class)
    public void testDeleteEntity() {
        Transaction tx = neo4jTemplate.beginTx();
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        neo4jTemplate.delete(spouse);
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        Person spouseFromIndex = personRepository.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index", spouseFromIndex);
        Assert.assertNull("node deleted " + id, neo4jTemplate.getNode(id));
    }

    @Test
    public void testPersistGenericEntity() {
        final Attribute<String> attribute = new Attribute<String>();
        attribute.setValue("test");
        persist(attribute);
    }
    @Test
    public void testAccessReadOnlyCollectionMakesEntityDirty() {
	    Group g = new Group();
	    g.getReadOnlyPersons();
	    g.persist();
    }
}
