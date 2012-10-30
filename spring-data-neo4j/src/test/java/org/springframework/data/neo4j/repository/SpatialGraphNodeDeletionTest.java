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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.conversion.Result;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.hamcrest.core.Is.*;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/repository/GraphRepositoryTest-context.xml"})
public class SpatialGraphNodeDeletionTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

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

//    @BeforeTransaction
//    public void cleanDb() {
//        Neo4jHelper.cleanDb(neo4jTemplate);
//    }
    
    @Before
    public void setUp() throws Exception {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                testTeam = new TestTeam();
                testTeam.createSDGTeam(personRepository, groupRepository,friendshipRepository);
                assertThat(countGeomNodes("personLayer"), is(2));
            }
        });
    }

    @Test
    public void testFindPeopleWithinDistance() {
    	
    	//Check that we have two entries in the spatial index
    	//Then remove one
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Iterable<Person> teamMembers = personRepository.findWithinDistance("personLayer", 16,56,70);
                assertThat(asCollection(teamMembers), hasItems(testTeam.michael, testTeam.david));
                assertThat(countGeomNodes("personLayer"), is(2));
                
                personRepository.delete(testTeam.david);
            }
        });
        
		// Now in a separate transaction, check that both the 'real' and geom
		// nodes have been removed
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	Iterable<Person> teamMembers = personRepository.findWithinDistance("personLayer", 16,56,70);
                assertThat(asCollection(teamMembers), hasItems(testTeam.michael));
                assertThat(countGeomNodes("personLayer"), is(1));
            }
        });
    }
    
	private int countGeomNodes(String layerName) {
		int count = 0;

		String statement = 
				"START root=node(0) " + 
				"MATCH root-[:SPATIAL]-> spatial-[:LAYER]->layer-[:GEOMETRIES]->geometry " + 
				"WHERE layer.layer = {layerName} " + 
				"RETURN geometry";

		// Use the above query to get the spatial node
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("layerName", layerName);
		Result<Map<String, Object>> query = neo4jTemplate.query(statement, params);

		// Convert it to a node
		EndResult<Node> result = query.to(Node.class);
		for (Node node : result) {
			count++;
		}

		return count;
	}
}
