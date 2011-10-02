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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.Friendship;
import org.springframework.data.neo4j.*;
import org.springframework.data.neo4j.FriendshipRepository;

import static org.junit.Assert.assertFalse;
import static org.springframework.data.neo4j.Person.persistedPerson;

import org.springframework.data.neo4j.Person;
import org.springframework.data.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.neo4j.support.node.Neo4jHelper;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

public class RelationshipEntityTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;
	@Autowired
	private GraphDatabaseService graphDatabaseService;
    @Autowired
    private FriendshipRepository friendshipRepository;

	@Autowired
	private DirectGraphRepositoryFactory graphRepositoryFactory;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testRelationshipCreate() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        Relationship rel = p.getPersistentState().getSingleRelationship(DynamicRelationshipType.withName("knows"), Direction.OUTGOING);
        assertEquals(f.getPersistentState(), rel);
        assertEquals(p2.getPersistentState(), rel.getEndNode());
    }

    @Test
    @Transactional
    public void shouldNotCreateSameRelationshipTwice() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        Friendship f2 = p.knows(p2);
        assertEquals(f, f2);
        assertEquals(1, IteratorUtil.count(p.getFriendships()));
    }

    @Test
    @Transactional
    public void testRelationshipSetProperty() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.setYears(1);
        assertEquals(1, f.getPersistentState().getProperty("Friendship.years"));
    }

    @Test
    @Transactional
    public void testRelationshipGetProperty() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        f.getPersistentState().setProperty("Friendship.years", 1);
        assertEquals(1, f.getYears());
    }

    @Test
    @Transactional
    public void testRelationshipGetStartNodeAndEndNode() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(p, f.getPerson1());
        assertEquals(p2, f.getPerson2());
    }

    @Test
    @Transactional
    public void testGetRelationshipToReturnsRelationship() {
        Person p = persistedPerson("Michael", 35);
        Person p2 = persistedPerson("David", 25);
        Friendship f = p.knows(p2);
        assertEquals(f,p.getRelationshipTo(p2, Friendship.class, "knows"));
    }

    @Test
    public void testRemoveRelationshipEntity() {
        cleanDb();
        Friendship f;
        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            Person p = persistedPerson("Michael", 35);
            Person p2 = persistedPerson("David", 25);
            f = p.knows(p2);
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        Transaction tx2 = graphDatabaseService.beginTx();
        try
        {
            f.remove();
            tx2.success();
        }
        finally
        {
            tx2.finish();
        }
        assertFalse("Unexpected relationship entity found.", friendshipRepository.findAll().iterator().hasNext());
    }

    @Test
    public void testRemoveRelationshipEntityIfNodeEntityIsRemoved() {
        cleanDb();
        Person p;
        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            p = persistedPerson("Michael", 35);
            Person p2 = persistedPerson("David", 25);
            p.knows(p2);
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        Transaction tx2 = graphDatabaseService.beginTx();
        try
        {
            p.remove();
            tx2.success();
        }
        finally
        {
            tx2.finish();
        }
        assertFalse("Unexpected relationship entity found.", friendshipRepository.findAll().iterator().hasNext());
    }
}
