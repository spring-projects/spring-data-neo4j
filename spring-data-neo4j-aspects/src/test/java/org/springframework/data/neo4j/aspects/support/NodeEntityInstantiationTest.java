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
import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/PersonDirectCreator-context.xml" })
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})

public class NodeEntityInstantiationTest extends EntityTestBase {

    @Test
    @Transactional
    public void testCreatePersonWithCreator() {
        Person p = persistedPerson("Rod", 39);
        long nodeId = getNodeId(p);

        Node node = neo4jTemplate.getNodeById(nodeId);
        Person person1 = (Person) neo4jTemplate.createEntityFromStoredType(node);
        assertEquals("Rod", person1.getName());
        Person person2 = neo4jTemplate.createEntityFromState(node,Person.class);
        assertEquals("Rod", person2.getName());

        GraphRepository<Person> finder = neo4jTemplate.repositoryFor(Person.class);
        Person found = finder.findOne(nodeId);
        assertEquals("Rod", found.getName());
    }
}
