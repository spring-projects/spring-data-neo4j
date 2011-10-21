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

import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/repository/GraphRepositoryTest-context.xml"})
@Transactional
public class SpatialGraphRepositoryTest {

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Autowired
    private PersonRepository personRepository;
    @Autowired
    GroupRepository groupRepository;

    @Autowired FriendshipRepository friendshipRepository;

    
    @Autowired
    PlatformTransactionManager transactionManager;
        
    private TestTeam testTeam;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }
    @Before
    public void setUp() throws Exception {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                testTeam = new TestTeam();
                testTeam.createSDGTeam(personRepository, groupRepository,friendshipRepository);
            }
        });
    }

    @Test
    public void testFindIterableOfPersonWithQueryAnnotationSpatial() {
        Iterable<Person> teamMembers = personRepository.findByBoundingBox( "personLayer", 55, 15, 57, 17 );
        assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david, testTeam.emil));
    }
}
