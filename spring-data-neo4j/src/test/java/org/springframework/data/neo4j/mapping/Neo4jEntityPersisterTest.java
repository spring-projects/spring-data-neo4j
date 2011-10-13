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
package org.springframework.data.neo4j.mapping;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.ManagedEntity;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 12.10.11
 */
public class Neo4jEntityPersisterTest extends Neo4jPersistentTestBase {

    @Test
    public void testCreateEntityFromStoredType() throws Exception {
        final Node personNode = gdc.createNode();
        personNode.setProperty("name","Michael");
        final Person person = entityPersister.createEntityFromState(personNode, Person.class);
        assertEquals("Michael",person.getName());
    }

    @Test
    public void testCreateEntityFromState() throws Exception {

    }

    @Test
    public void testProjectTo() throws Exception {
        storeInGraph(michael);
        final Developer developer = entityPersister.projectTo(michael, Developer.class);
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
        entityPersister.persist(michael);
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
}
