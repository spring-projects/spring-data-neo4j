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
import org.mockito.Mockito;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.mapping.ManagedEntity;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Named;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.helpers.collection.IteratorUtil.first;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 17.10.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:template-config-context.xml"})
public class EntityNeo4jTemplateTests extends EntityTestBase {

    public static final DynamicRelationshipType KNOWS = DynamicRelationshipType.withName("knows");

    @Autowired
    PlatformTransactionManager transactionManager;
    private Neo4jOperations neo4jOperations;

    @Before
    public void setUp() throws Exception {
        createTeam();
        neo4jOperations = template;
    }

    @Test @Transactional
    public void testRepositoryFor() throws Exception {
        final GraphRepository<Person> personRepository = neo4jOperations.repositoryFor(Person.class);
        final GraphRepository<Group> groupRepository = neo4jOperations.repositoryFor(Group.class);
        final GraphRepository<Friendship> friendshipRepository = neo4jOperations.repositoryFor(Friendship.class);
        testTeam.createSDGTeam(personRepository,groupRepository,friendshipRepository);
        final Person found = personRepository.findOne(testTeam.michael.getId());
        assertEquals(found.getId(),testTeam.michael.getId());
    }

    @Test @Transactional
    public void testRelationshipRepositoryFor() throws Exception {
        
        final GraphRepository<Friendship> friendshipRepository = neo4jOperations.repositoryFor(Friendship.class);
        final Friendship found = friendshipRepository.findOne(testTeam.friendShip.getId());
        assertEquals(found.getId(),testTeam.friendShip.getId());
    }

    @Test @Transactional @Ignore
    public void testGetIndexForType() throws Exception {
        
        final Index<PropertyContainer> personIndex = template.getIndex(Person.class);
        assertEquals("Person",personIndex.getName());
    }

    @Test @Transactional
    public void testGetIndexForName() throws Exception {
        
        final Index<PropertyContainer> nameIndex = template.getIndex(Person.NAME_INDEX);
        assertEquals(Person.NAME_INDEX, nameIndex.getName());
    }

    @Test @Transactional
    public void testGetIndexForNoTypeAndName() throws Exception {
        
        final Index<PropertyContainer> nameIndex = neo4jOperations.getIndex(Person.NAME_INDEX,null);
        assertEquals(Person.NAME_INDEX,nameIndex.getName());
    }

    @Test @Transactional @Ignore
    public void testGetIndexForTypeAndNoName() throws Exception {
        
        final Index<PropertyContainer> nameIndex = neo4jOperations.getIndex(null,Person.class);
        assertEquals("Person",nameIndex.getName());
    }
    @Test @Transactional
    public void testGetIndexForTypeAndName() throws Exception {
        
        final Index<PropertyContainer> nameIndex = neo4jOperations.getIndex(Person.NAME_INDEX,Person.class);
        assertEquals(Person.NAME_INDEX, nameIndex.getName());
    }

    @Test @Transactional
    public void testFindOne() throws Exception {
        
        final Person found = neo4jOperations.findOne(testTeam.michael.getId(), Person.class);
        assertEquals(testTeam.michael.getId(), found.getId());
        assertEquals(testTeam.michael.getName(), found.getName());
    }

    @Test @Transactional
    public void testFindAll() throws Exception {
        final Collection<Person> people = asCollection(neo4jOperations.findAll(Person.class));
        assertEquals(3,people.size());
        assertNotNull("people attributes where loaded",first(people).getName());
    }

    @Test @Transactional
    public void testCount() throws Exception {
        
        assertEquals(3,neo4jOperations.count(Person.class));
    }

    @Test @Transactional
    public void testCreateRelationshipEntityFromStoredType() throws Exception {
        
        final Relationship friendshipRelationship = getRelationshipState(testTeam.friendShip);
        Friendship found = template.createEntityFromStoredType(friendshipRelationship, template.getMappingPolicy(testTeam.michael));
        assertEquals(testTeam.friendShip.getId(),found.getId());
    }

    @Test @Transactional
    public void testCreateNodeEntityFromStoredType() throws Exception {
        
        final Node michaelNode = getNodeState(testTeam.michael);
        Person found = template.createEntityFromStoredType(michaelNode, template.getMappingPolicy(testTeam.michael));
        assertEquals(testTeam.michael.getId(),found.getId());
    }

    @Test @Transactional
    public void testCreateEntityFromState() throws Exception {
        
        final PropertyContainer michaelNode = getNodeState(testTeam.michael);
        Person found = template.createEntityFromStoredType(michaelNode, template.getMappingPolicy(testTeam.michael));
        assertEquals(testTeam.michael.getId(),found.getId());
    }

    @Test @Transactional
    public void testProjectTo() throws Exception {
        final Named named = neo4jOperations.projectTo(testTeam.sdg, Named.class);
        assertEquals(testTeam.sdg.getName(),named.getName());
    }

    @Test @Transactional
    public void testGetPersistentState() throws Exception {
        assertEquals(testTeam.michael.getId(),(Long)((Node)neo4jOperations.getPersistentState(testTeam.michael)).getId());
    }

    @Test @Transactional
    public void testSetPersistentState() throws Exception {
        final Person clone = new Person();
        template.setPersistentState(clone, neo4jOperations.getPersistentState(testTeam.david));
        assertEquals(testTeam.david.getId(), clone.getId());
    }

    @Test(expected = DataRetrievalFailureException.class)
    public void testDelete() throws Exception {
        final Long id = new TransactionTemplate(transactionManager).execute(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus transactionStatus) {
                final Long id = testTeam.michael.getId();
                neo4jOperations.delete(testTeam.michael);
                return id;
            }
        });
        try (Transaction tx=graphDatabaseService.beginTx()) {
            assertNull(neo4jOperations.getNode(id));
            tx.success();
        }
    }

    @Test(expected = DataRetrievalFailureException.class)
    public void testRemoveNodeEntity() throws Exception {
        final Long id =
        new TransactionTemplate(transactionManager).execute(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus transactionStatus) {
                final Long id = testTeam.michael.getId();
                template.delete(testTeam.michael);
                return id;
            }
        });
        try (Transaction tx=graphDatabaseService.beginTx()) {
            assertNull(neo4jOperations.getNode(id));
            tx.success();
        }
    }

    @Test(expected = DataRetrievalFailureException.class)
    public void testRemoveRelationshipEntity() throws Exception {
        final Long id =
        new TransactionTemplate(transactionManager).execute(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus transactionStatus) {
                Long id = testTeam.friendShip.getId();
                template.delete(testTeam.friendShip);
                return id;
            }
        });
        try (Transaction tx=graphDatabaseService.beginTx()) {
            assertNull(neo4jOperations.getRelationship(id));
            tx.success();
        }

    }


    @Test @Transactional
    public void testCreateNodeAs() throws Exception {
        final Person thomas = neo4jOperations.createNodeAs(Person.class, map("name", "Thomas"));
        assertEquals("Thomas",neo4jOperations.getNode(thomas.getId()).getProperty("name"));
        final Person found = template.createEntityFromStoredType(getNodeState(thomas), template.getMappingPolicy(Person.class));
        assertEquals("Thomas",found.getName());
    }

    @Test @Transactional
    public void testIsNodeEntity() throws Exception {
        assertEquals(true, template.isNodeEntity(Person.class));
        assertEquals(false, template.isNodeEntity(Friendship.class));
        assertEquals(false, template.isNodeEntity(Object.class));
    }

    @Test @Transactional
    public void testIsRelationshipEntity() throws Exception {
        assertEquals(true, template.isRelationshipEntity(Friendship.class));
        assertEquals(false, template.isRelationshipEntity(Person.class));
        assertEquals(false, template.isRelationshipEntity(Object.class));
    }

    @Test @Transactional
    public void testSave() throws Exception {
        final Person thomas = new Person("Thomas", 30);
        neo4jOperations.save(thomas);
        final Node node = getNodeState(thomas);
        assertNotNull("created node",node);
        assertEquals("created node with id", (Long) node.getId(), thomas.getId());
        assertEquals("created node with name", "Thomas", node.getProperty("name"));
    }

    static abstract class ManagedTestEntity implements ManagedEntity {}
    @Test @Transactional
    public void testIsManaged() throws Exception {
        assertEquals(true, template.isManaged(Mockito.mock(ManagedEntity.class)));
        assertEquals(true, template.isManaged(Mockito.mock(ManagedTestEntity.class)));
        assertEquals(false, template.isManaged(testTeam.michael));
        assertEquals(false, template.isManaged(testTeam.friendShip));
        assertEquals(false, template.isManaged(new Object()));
    }

    @Test @Transactional
    public void testQuery() throws Exception {
        final Person result = neo4jOperations.query("start n=node({self}) return n", map("self", testTeam.michael.getId())).to(Person.class).single();
        assertEquals(testTeam.michael.getId(),result.getId());

    }

    @Test @Transactional
    public void testGetRelationshipBetweenNodes() throws Exception {
        final Relationship knows = neo4jOperations.getRelationshipBetween(getNodeState(testTeam.michael), getNodeState(testTeam.david), "knows");
        assertEquals(testTeam.friendShip.getId(),(Long)knows.getId());
    }
    @Test @Transactional

    public void testGetAutoPersistedRelationshipBetweenNodes() throws Exception {
        final Node emilNode = getNodeState(testTeam.emil);
        final Node michaelNode = getNodeState(testTeam.michael);
        final Relationship boss = neo4jOperations.getRelationshipBetween(emilNode, michaelNode, "boss");
        assertNotNull("found relationship",boss);
        assertEquals(michaelNode,boss.getEndNode());
        assertEquals(emilNode,boss.getStartNode());
    }

    @Test @Transactional
    public void testGetRelationshipBetween() throws Exception {
        final Friendship knows = neo4jOperations.getRelationshipBetween(testTeam.michael, testTeam.david, Friendship.class, "knows");
        assertEquals(testTeam.friendShip.getId(),knows.getId());
    }
    @Test @Transactional
    public void testGetMultipleRelationshipBetween() throws Exception {
        final Friendship friendship = neo4jOperations.getRelationshipBetween(testTeam.michael, testTeam.david, Friendship.class, "knows");
        final Friendship friendship2 = neo4jOperations.createRelationshipBetween(testTeam.michael, testTeam.david, Friendship.class, "knows", true);
        final Iterable<Friendship> allFriendships = neo4jOperations.getRelationshipsBetween(testTeam.michael, testTeam.david, Friendship.class, "knows");
        assertThat(allFriendships, hasItems(friendship, friendship2));
    }

    @Test @Transactional
    public void testDeleteRelationshipBetween() throws Exception {
        neo4jOperations.deleteRelationshipBetween(testTeam.michael,testTeam.david,"knows");
        assertNull("relationship deleted", getNodeState(testTeam.michael).getSingleRelationship(KNOWS, OUTGOING));
    }

    @Test @Transactional
    public void testCreateRelationshipBetweenNodes() throws Exception {
        final Friendship friendship = neo4jOperations.createRelationshipBetween(testTeam.david, testTeam.emil, Friendship.class, "knows", false);
        assertEquals(friendship.getId(),(Long)getNodeState(testTeam.david).getSingleRelationship(KNOWS, OUTGOING).getId());
        final List<Friendship> friendships = IteratorUtil.addToCollection(friendshipRepository.findAll(), new ArrayList<Friendship>());
        assertEquals(2,friendships.size());
        assertThat(friendships, hasItems(testTeam.friendShip, friendship));
    }

    @Test @Transactional
    public void testCreateDuplicateRelationshipBetweenNodes() throws Exception {
        neo4jOperations.createRelationshipBetween(testTeam.michael, testTeam.david, Friendship.class, "knows", true);
        assertEquals(2, asCollection(getNodeState(testTeam.michael).getRelationships(KNOWS, OUTGOING)).size());
    }

    @Test @Transactional
    public void testCreateRelationshipBetween() throws Exception {
        final Node davidNode = getNodeState(testTeam.david);
        final Relationship friendship = neo4jOperations.createRelationshipBetween(davidNode, getNodeState(testTeam.emil), "knows", MapUtil.map("years", 10));
        assertEquals(friendship.getId(),davidNode.getSingleRelationship(KNOWS, OUTGOING).getId());
        assertEquals(10,friendship.getProperty("years"));

    }

    @Test @Transactional
    public void testConvertSingle() throws Exception {
        final Person p = neo4jOperations.convert(neo4jOperations.getPersistentState(testTeam.michael), Person.class);
        assertEquals(testTeam.michael.getName(),p.getName());
    }

    @Test @Transactional
    public void testConvert() throws Exception {
        final Result<Group> groups = neo4jOperations.convert(Arrays.asList(getNodeState(testTeam.sdg))).to(Group.class);
        assertEquals(testTeam.sdg.getName(),groups.iterator().next().getName());
    }

    @Test @Transactional
    public void testQueryEngineForCypher() throws Exception {
        final CypherQueryEngine engine = neo4jOperations.queryEngineFor();
        final Person result = engine.query("start n=node({self}) return n", map("self", testTeam.michael.getId())).to(Person.class).single();
        assertEquals(testTeam.michael.getId(), result.getId());
    }

    @Test @Transactional
    public void testTraverse() throws Exception {
//        final TraversalDescription traversalDescription = neo4jOperations.traversalDescription().relationships(DynamicRelationshipType.withName("knows"), Direction.OUTGOING).filter(Traversal.returnAllButStartNode());
        final TraversalDescription traversalDescription = neo4jOperations.traversalDescription().relationships(DynamicRelationshipType.withName("knows"), Direction.OUTGOING).evaluator(Evaluators.excludeStartPosition());
        final Person knows = neo4jOperations.traverse(testTeam.michael, traversalDescription).to(Person.class).single();
        assertEquals(testTeam.david.getName(), knows.getName());
    }

    @Test @Transactional
    public void testLookup() throws Exception {
        final Person found = neo4jOperations.lookup(Person.class, "name","name:Michael").to(Person.class).single();
        assertEquals(testTeam.michael.getId(),found.getId());
    }

    @Test @Transactional
    public void testLookupExact() throws Exception {
        final Person found = neo4jOperations.lookup(Person.class, "name","Michael").to(Person.class).single();
        assertEquals(testTeam.michael.getId(),found.getId());
    }

    @Test(expected = InvalidDataAccessApiUsageException.class)
    @Transactional
    public void testLookupExactLabelIndex() throws Exception {
        final Person found = neo4jOperations.lookup(Person.class, "alias","michaelAlias").to(Person.class).single();
        assertEquals(testTeam.michael.getId(),found.getId());
    }

    @Test
    @Transactional
    public void testFindAllSchemaIndex() throws Exception {
        final Person found = neo4jOperations.findByIndexedValue(Person.class, "alias", "michaelAlias").single();
        assertEquals(testTeam.michael.getId(),found.getId());
    }
}
