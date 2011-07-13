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
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.Person;
import org.springframework.data.neo4j.Person;
import org.springframework.data.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.neo4j.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:org/springframework/data/neo4j/support/Neo4jGraphPersistenceTest-context.xml",
        "classpath:org/springframework/data/neo4j/support/PersonDirectCreator-context.xml" })
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})

    public class NodeEntityInstantiationTest {

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
    public void testCreatePersonWithCreator() {
        Person p = persistedPerson("Rod", 39);
        long nodeId = p.getNodeId();

        Node node = graphDatabaseContext.getNodeById(nodeId);
        Person person1 = (Person) graphDatabaseContext.createEntityFromStoredType(node);
        assertEquals("Rod", person1.getName());
        Person person2 = graphDatabaseContext.createEntityFromState(node,Person.class);
        assertEquals("Rod", person2.getName());

        GraphRepository<Person> finder = graphRepositoryFactory.createGraphRepository(Person.class);
        Person found = finder.findOne(nodeId);
        assertEquals("Rod", found.getName());
    }
}
