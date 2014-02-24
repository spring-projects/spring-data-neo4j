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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.Traversal;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class TraversalTests extends EntityTestBase {

    @Test
    @Transactional
    public void testTraverseFromGroupToPeople() {
        Person p = persistedPerson("Michael", 35);
        Group group = persist(new Group());
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = Traversal.description().relationships(DynamicRelationshipType.withName("persons")).evaluator(Evaluators.excludeStartPosition());
        Iterable<Person> people = neo4jTemplate.<Person>traverse(group, Person.class, traversalDescription);
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        assertEquals(Collections.singleton(p),found);
    }
    @SuppressWarnings("unchecked")
    @Test
    @Transactional
    public void testTraverseFromGroupToPeoplePaths() {
        Person p = persistedPerson("Michael", 35);
        Group group = persist(new Group());
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = Traversal.description().relationships(DynamicRelationshipType.withName("persons"), Direction.OUTGOING).evaluator(Evaluators.excludeStartPosition());
        Iterable<EntityPath<Group,Person>> paths = (Iterable<EntityPath<Group, Person>>) neo4jTemplate.<EntityPath<Group,Person>>traverse(group, EntityPath.class, traversalDescription);
        for (EntityPath<Group, Person> path : paths) {
            assertEquals(group, path.startEntity());
            assertEquals(p, path.endEntity());
            assertEquals(1,path.length());
        }
    }

    @Test
    @Transactional
    public void testTraverseFieldFromGroupToPeople() {
        Person p = persistedPerson("Michael", 35);
        Group group = persist(new Group());
        group.addPerson(p);
        assertEquals(Collections.singletonList(p),IteratorUtil.asCollection(group.getPeople()));
    }

//    @Ignore("TODO - add back when strict setting working properly again in AbstractMappingContext.getPersistentEntity")
    @Test
    @Transactional
    public void testTraverseFieldFromGroupToPeopleNodes() {
        Person p = persistedPerson("Michael", 35);
        Group group = persist(new Group());
        group.addPerson(p);
        assertEquals(Collections.singletonList(getNodeState(p)), IteratorUtil.asCollection(group.getPeopleNodes()));
    }

//    @Ignore("TODO - add back when strict setting working properly again in AbstractMappingContext.getPersistentEntity")
    @Test
    @Transactional
    public void testTraverseFieldFromGroupToPeopleRelationships() {
        Person p = persistedPerson("Michael", 35);
        Group group = persist(new Group());
        group.addPerson(p);
        Relationship personRelationship = getNodeState(group).getSingleRelationship(DynamicRelationshipType.withName("persons"), Direction.OUTGOING);
        assertEquals(Collections.singletonList(personRelationship), IteratorUtil.asCollection(group.getPeopleRelationships()));
    }

    @Test
    @Transactional
    public void testTraverseFromGroupToPeopleWithFinder() {
        final GraphRepository<Person> finder = neo4jTemplate.repositoryFor(Person.class);
        Person p = persistedPerson("Michael", 35);
        Group group = persist(new Group());
        group.setName("dev");
        group.addPerson(p);
        final TraversalDescription traversalDescription = Traversal.description().relationships(DynamicRelationshipType.withName("persons")).evaluator(Evaluators.excludeStartPosition());
        Iterable<Person> people = finder.findAllByTraversal(group, traversalDescription);
        final HashSet<Person> found = new HashSet<Person>();
        for (Person person : people) {
            found.add(person);
        }
        assertEquals(Collections.singleton(p),found);
    }

}
