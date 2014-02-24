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
package org.springframework.data.neo4j.mapping.persist;

import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.mapping.ManagedEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentTestBase;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

/**
 * @author mh
 * @since 12.10.11
 */
public class Neo4jEntityPersisterTests extends Neo4jPersistentTestBase {

    @Override
    protected void setBasePackage(Neo4jMappingContext mappingContext) throws ClassNotFoundException {
        super.setBasePackage(mappingContext,Person.class.getPackage().getName(),Developer.class.getPackage().getName());
    }

    @Test
    public void testCreateEntityFromStoredType() throws Exception {
        final Node personNode = template.createNode();
        personNode.setProperty("name","Michael");
        final Person person = entityPersister.createEntityFromState(personNode, Person.class, template.getMappingPolicy(Person.class), template);
        assertEquals("Michael",person.getName());
    }

    @Test
    public void testCreateEntityFromState() throws Exception {

    }

    @Test
    public void testProjectTo() throws Exception {
        storeInGraph(michael);
        final Developer developer = entityPersister.projectTo(michael, Developer.class, template);
        assertEquals(michael.getId(),  developer.id);
        assertEquals(michael.getName(),  developer.name);
    }

    @Test
    public void testGetPersistentState() throws Exception {
        storeInGraph(michael);
        assertEquals(michaelNode(), entityPersister.getPersistentState(michael));
    }

    @Test
    public void testPersist() throws Exception {
        entityPersister.persist(michael, template.getMappingPolicy(michael), template, null );
        assertEquals((Long) michaelNode().getId(), michael.getId());
        assertEquals(michaelNode(), entityPersister.getPersistentState(michael));
        assertEquals(michaelNode().getProperty("name"), michael.getName());

    }

    @Test
    public void testIsManaged() throws Exception {
        assertEquals(false,entityPersister.isManaged(michael));
        assertEquals(true,entityPersister.isManaged(Mockito.mock(ManagedEntity.class)));
    }

    @Test
    public void testIsNodeEntity() throws Exception {
        assertEquals(true,entityPersister.isNodeEntity(Person.class));
        assertEquals(false, entityPersister.isNodeEntity(Friendship.class));
    }

    @Test
    public void testIsRelationshipEntity() throws Exception {
        assertEquals(false,entityPersister.isRelationshipEntity(Person.class));
        assertEquals(true, entityPersister.isRelationshipEntity(Friendship.class));

    }

    @Test
    @Transactional
    public void testFetchSingleEntity() {
        final Node node = template.createNode();
        node.setProperty("name","Fetch");
        final Person p = new Person(node.getId());
        template.fetch(p);
        assertEquals("Fetch",p.getName());
    }
    @Test
    @Transactional
    public void testFetchEntityCollection() {
        final Node node = template.createNode();
        node.setProperty("name","Fetch");
        final Person p = new Person(node.getId());
        template.fetch(asList(p));
        assertEquals("Fetch",p.getName());
    }
}
