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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.Traversal;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Named;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

/**
 * @author mh
 * @since 17.10.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:template-config-context.xml"})
@Transactional
public class EntityNeo4jTemplateTest extends EntityTestBase {

    public static final DynamicRelationshipType KNOWS = DynamicRelationshipType.withName("knows");

    @Before
    public void setUp() throws Exception {
        createTeam();
    }

    @Test
    public void testRepositoryFor() throws Exception {
        final GraphRepository<Person> personRepository = neo4jTemplate.repositoryFor(Person.class);
        final GraphRepository<Group> groupRepository = neo4jTemplate.repositoryFor(Group.class);
        final GraphRepository<Friendship> friendshipRepository = neo4jTemplate.repositoryFor(Friendship.class);
        testTeam.createSDGTeam(personRepository,groupRepository,friendshipRepository);
        final Person found = personRepository.findOne(testTeam.michael.getId());
        assertEquals(found.getId(),testTeam.michael.getId());
    }

    @Test
    public void testRelationshipRepositoryFor() throws Exception {
        
        final GraphRepository<Friendship> friendshipRepository = neo4jTemplate.repositoryFor(Friendship.class);
        final Friendship found = friendshipRepository.findOne(testTeam.friendShip.getId());
        assertEquals(found.getId(),testTeam.friendShip.getId());
    }

    @Test
    public void testGetIndexForType() throws Exception {
        
        final Index<PropertyContainer> personIndex = neo4jTemplate.getIndex(Person.class);
        assertEquals("Person",personIndex.getName());
    }

    @Test
    public void testGetIndexForName() throws Exception {
        
        final Index<PropertyContainer> nameIndex = neo4jTemplate.getIndex(Person.NAME_INDEX);
        assertEquals(Person.NAME_INDEX, nameIndex.getName());
    }

    @Test
    public void testGetIndexForNoTypeAndName() throws Exception {
        
        final Index<PropertyContainer> nameIndex = neo4jTemplate.getIndex(null,Person.NAME_INDEX);
        assertEquals(Person.NAME_INDEX,nameIndex.getName());
    }

    @Test
    public void testGetIndexForTypeAndNoName() throws Exception {
        
        final Index<PropertyContainer> nameIndex = neo4jTemplate.getIndex(Person.class,null);
        assertEquals("Person",nameIndex.getName());
    }
    @Test
    public void testGetIndexForTypeAndName() throws Exception {
        
        final Index<PropertyContainer> nameIndex = neo4jTemplate.getIndex(Person.class,Person.NAME_INDEX);
        assertEquals(Person.NAME_INDEX, nameIndex.getName());
    }

    @Test
    public void testFindOne() throws Exception {
        
        final Person found = neo4jTemplate.findOne(testTeam.michael.getId(), Person.class);
        assertEquals(found.getId(),testTeam.michael.getId());
    }

    @Test
    public void testFindAll() throws Exception {
        
        final Collection<Person> people = asCollection(neo4jTemplate.findAll(Person.class));
        assertEquals(3,people.size());
    }

    @Test
    public void testCount() throws Exception {
        
        assertEquals(3,neo4jTemplate.count(Person.class));
    }

    @Test
    public void testCreateRelationshipEntityFromStoredType() throws Exception {
        
        final Relationship friendshipRelationship = getRelationshipState(testTeam.friendShip);
        Friendship found = neo4jTemplate.createEntityFromStoredType(friendshipRelationship);
        assertEquals(testTeam.friendShip.getId(),found.getId());
    }

    @Test
    public void testCreateNodeEntityFromStoredType() throws Exception {
        
        final Node michaelNode = getNodeState(testTeam.michael);
        Person found = neo4jTemplate.createEntityFromStoredType(michaelNode);
        assertEquals(testTeam.michael.getId(),found.getId());
    }

    @Test
    public void testCreateEntityFromState() throws Exception {
        
        final PropertyContainer michaelNode = getNodeState(testTeam.michael);
        Person found = neo4jTemplate.createEntityFromStoredType(michaelNode);
        assertEquals(testTeam.michael.getId(),found.getId());
    }

    @Test
    public void testProjectTo() throws Exception {
        final Named named = neo4jTemplate.projectTo(testTeam.sdg, Named.class);
        assertEquals(testTeam.sdg.getName(),named.getName());
    }

    @Test
    public void testGetPersistentState() throws Exception {
        assertEquals(testTeam.michael.getId(),(Long)((Node)neo4jTemplate.getPersistentState(testTeam.michael)).getId());
    }

    @Test
    public void testSetPersistentState() throws Exception {

    }


    @Test
    @Ignore("TODO execute non tx")
    public void testDelete() throws Exception {
        final Long id = testTeam.michael.getId();
        neo4jTemplate.delete(testTeam.michael);
        assertNull(neo4jTemplate.getGraphDatabase().getNodeById(id));
    }

    @Test
    @Ignore("TODO execute non tx")
    public void testRemoveNodeEntity() throws Exception {
        final Long id = testTeam.michael.getId();
        neo4jTemplate.removeNodeEntity(testTeam.michael);
        assertNull(neo4jTemplate.getGraphDatabase().getNodeById(id));
    }

    @Test
    @Ignore("TODO execute non tx")
    public void testRemoveRelationshipEntity() throws Exception {
        final Long id = testTeam.friendShip.getId();
        neo4jTemplate.removeRelationshipEntity(testTeam.friendShip);
        assertNull(neo4jTemplate.getGraphDatabase().getRelationshipById(id));

    }


    @Test
    public void testCreateNodeAs() throws Exception {

    }

    @Test
    public void testIsNodeEntity() throws Exception {

    }

    @Test
    public void testIsRelationshipEntity() throws Exception {

    }

    @Test
    public void testSave() throws Exception {

    }

    @Test
    public void testIsManaged() throws Exception {

    }

    @Test
    public void testQuery() throws Exception {

    }

    @Test
    public void testGetRelationshipBetweenNodes() throws Exception {
        
        final Relationship knows = neo4jTemplate.getRelationshipBetween(getNodeState(testTeam.michael), getNodeState(testTeam.david), "knows");
        assertEquals(testTeam.friendShip.getId(),(Long)knows.getId());
    }
    @Test

    public void testGetAutoPersistedRelationshipBetweenNodes() throws Exception {
        
        final Node emilNode = getNodeState(testTeam.emil);
        final Node michaelNode = getNodeState(testTeam.michael);
        final Relationship boss = neo4jTemplate.getRelationshipBetween(emilNode, michaelNode, "boss");
        assertNotNull("found relationship",boss);
        assertEquals(michaelNode,boss.getEndNode());
        assertEquals(emilNode,boss.getStartNode());
    }

    @Test
    public void testGetRelationshipBetween() throws Exception {
        
        final Friendship knows = neo4jTemplate.getRelationshipBetween(testTeam.michael, testTeam.david, Friendship.class, "knows");
        assertEquals(testTeam.friendShip.getId(),knows.getId());
    }

    @Test
    public void testDeleteRelationshipBetween() throws Exception {
        
        neo4jTemplate.deleteRelationshipBetween(testTeam.michael,testTeam.david,"knows");
        assertNull("relationship deleted", getNodeState(testTeam.michael).getSingleRelationship(KNOWS, OUTGOING));
    }

    @Test
    public void testCreateRelationshipBetweenNodes() throws Exception {
        
        final Friendship friendship = neo4jTemplate.createRelationshipBetween(testTeam.david, testTeam.emil, Friendship.class, "knows", false);
        assertEquals(friendship.getId(),(Long)getNodeState(testTeam.david).getSingleRelationship(KNOWS, OUTGOING).getId());
    }
    @Test
    public void testCreateDuplicateRelationshipBetweenNodes() throws Exception {
        
        neo4jTemplate.createRelationshipBetween(testTeam.michael, testTeam.david, Friendship.class, "knows", true);
        assertEquals(2, asCollection(getNodeState(testTeam.michael).getRelationships(KNOWS, OUTGOING)).size());
    }

    @Test
    public void testCreateRelationshipBetween() throws Exception {
        
        final Node davidNode = getNodeState(testTeam.david);
        final Relationship friendship = neo4jTemplate.createRelationshipBetween(davidNode, getNodeState(testTeam.emil), "knows", MapUtil.map("years", 10));
        assertEquals(friendship.getId(),davidNode.getSingleRelationship(KNOWS, OUTGOING).getId());
        assertEquals(10,friendship.getProperty("years"));

    }

    @Test
    public void testConvertSingle() throws Exception {
        
        final Person p = neo4jTemplate.convert(neo4jTemplate.getPersistentState(testTeam.michael), Person.class);
        assertEquals(testTeam.michael.getName(),p.getName());
    }

    @Test
    public void testConvert() throws Exception {
        final EndResult<Group> groups = neo4jTemplate.convert(Arrays.asList(getNodeState(testTeam.sdg))).to(Group.class);
        assertEquals(testTeam.sdg.getName(),groups.iterator().next().getName());
    }

    @Test
    public void testQueryEngineFor() throws Exception {

    }

    @Test
    public void testTraverse() throws Exception {
        
        final TraversalDescription traversalDescription = neo4jTemplate.traversalDescription().relationships(DynamicRelationshipType.withName("knows"), Direction.OUTGOING).filter(Traversal.returnAllButStartNode());
        final Person knows = neo4jTemplate.traverse(testTeam.michael, traversalDescription).to(Person.class).single();
        assertEquals(testTeam.david.getName(), knows.getName());
    }

    @Test
    public void testLookup() throws Exception {

    }
}
