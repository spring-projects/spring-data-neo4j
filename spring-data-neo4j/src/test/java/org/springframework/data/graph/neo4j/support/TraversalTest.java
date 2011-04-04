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

package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.repository.NodeGraphRepository;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

public class TraversalTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private DirectGraphRepositoryFactory graphRepositoryFactory;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testTraverseFromGroupToPeople() {
        Person p = persistedPerson("Michael", 35);
        Group group = new Group().persist();
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = Traversal.description().relationships(DynamicRelationshipType.withName("persons")).evaluator(Evaluators.excludeStartPosition());
        Iterable<Person> people = (Iterable<Person>) group.findAllByTraversal(Person.class, traversalDescription);
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        assertEquals(Collections.singleton(p),found);
    }
    @Test
    @Transactional
    public void testTraverseFromGroupToPeoplePaths() {
        Person p = persistedPerson("Michael", 35);
        Group group = new Group().persist();
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = Traversal.description().relationships(DynamicRelationshipType.withName("persons"), Direction.OUTGOING).evaluator(Evaluators.excludeStartPosition());
        Iterable<EntityPath<Group,Person>> paths = group.<Group,Person>findAllPathsByTraversal(traversalDescription);
        for (EntityPath<Group, Person> path : paths) {
            assertEquals(group, path.startEntity());
            assertEquals(p, path.endEntity());
            assertEquals(1,path.length());
        }
    }

    @Test
    @Transactional
    @Rollback(false)
    public void testTraverseFieldFromGroupToPeople() {
        Person p = persistedPerson("Michael", 35);
        Group group = new Group().persist();
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
        final NodeGraphRepository<Person> finder = graphRepositoryFactory.createNodeEntityRepository(Person.class);
        Person p = persistedPerson("Michael", 35);
        Group group = new Group().persist();
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

}
