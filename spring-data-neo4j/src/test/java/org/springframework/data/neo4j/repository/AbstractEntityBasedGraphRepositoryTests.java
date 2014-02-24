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

package org.springframework.data.neo4j.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.mapping.InvalidEntityTypeException;
import org.springframework.data.neo4j.model.AbstractNodeEntity;
import org.springframework.data.neo4j.model.Concrete1NodeEntity;
import org.springframework.data.neo4j.model.Concrete2NodeEntity;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repositories.AbstractNodeEntityRepository;
import org.springframework.data.neo4j.repositories.PersonRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for repositories which are defined against an Abstract Entity.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class AbstractEntityBasedGraphRepositoryTests {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private Neo4jTemplate neo4jTemplate;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    AbstractNodeEntityRepository abstractNodeEntityRepository;
    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }


    @Transactional
    @Test(expected = InvalidEntityTypeException.class)  // DATAGRAPH-298
    public void testInvalidEntityLoadAttemptThroughAbstractRepoThrowsAppropriateException() {

        Person person = personRepository.save(new Person("someone",30));

        // We are trying to load a Person Node Entity, using a completely different
        // and abstract defined Node entity - this should fail
        AbstractNodeEntity shouldNotWork = abstractNodeEntityRepository.findOne(person.getId());

    }

    @Test
    @Transactional  // DATAGRAPH-298
    public void testConcreteEntityLoadedThroughAbstractRepoLoadsCorrectType() {

        Concrete1NodeEntity origConcrete1 = new Concrete1NodeEntity("concrete1A");
        Concrete2NodeEntity origConcrete2 = new Concrete2NodeEntity("concrete2A");
        abstractNodeEntityRepository.save(origConcrete1);
        abstractNodeEntityRepository.save(origConcrete2);

        Concrete1NodeEntity loadedConcrete1 = (Concrete1NodeEntity)abstractNodeEntityRepository.findOne(origConcrete1.id);
        assertNotNull(loadedConcrete1);
        assertThat(loadedConcrete1, is(origConcrete1));

        Concrete2NodeEntity loadedConcrete2 = (Concrete2NodeEntity)abstractNodeEntityRepository.findOne(origConcrete2.id);
        assertNotNull(loadedConcrete2);
        assertThat(loadedConcrete2, is(origConcrete2));

    }



}
