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

package org.springframework.data.neo4j.rest.integration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.rest.graphdb.query.CypherTransactionExecutionException;
import org.springframework.data.neo4j.aspects.support.GraphRepositoryTests;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
* @author mh
* @since 28.03.11
*/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
    "classpath:RestTests-context.xml"})
public class RestGraphRepositoryTests extends GraphRepositoryTests {

    @BeforeClass
    public static void startDb() throws Exception {
        RestTestBase.startDb();
    }

    @BeforeTransaction
    public void cleanDb() {
        RestTestBase.cleanDb();
    }
    @AfterClass
    public static void shutdownDb() {
        RestTestBase.shutdownDb();
    }

    // TODO - Change REST to have a better (more descriptive)
    //        exception thrown when duplicate violations occur
    /*
    Unfortunately the REST scenario does not provide us with enough info to work out that this
    was a constraint violation (other than parsing the message itself which is not great) as it
    merely throws an IllegalStateException when no content is found for a response when adding
    labels. The org.neo4j.rest.graphdb.ExecutingRestAPI.addLabels(RestNode node, String...labels)
    method is the current offender in this case. The REST API itself would probably need to change for
    us to be able to deal with this appropriately in SDN
    */
    @Test(expected = CypherTransactionExecutionException.class)
    public void testSaveWhenFailOnDuplicateSetToTrue() {
          super.testSaveWhenFailOnDuplicateSetToTrue();
    }

}
