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

import org.junit.*;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.rest.graphdb.query.CypherTransactionExecutionException;
import org.springframework.data.neo4j.aspects.support.NodeEntityTests;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
* @author mh
* @since 28.03.11
*/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
        "classpath:RestTests-context-index.xml"})
public class RestNodeEntityTests extends NodeEntityTests {

    @BeforeClass
    public static void startDb() throws Exception {
        RestTestBase.startDb();
    }

    @Before
    public void cleanDb() {
        RestTestBase.cleanDb();
    }

    @AfterClass
    public static void shutdownDb() {
        RestTestBase.shutdownDb();
    }

    @Override
    @Ignore
    public void testSetShortProperty() {
        // super.testSetShortProperty();
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultFailOnDuplicateSetToTrueCausesExceptionWhenAnotherDuplicateEntityCreated() {
        super.testDefaultFailOnDuplicateSetToTrueCausesExceptionWhenAnotherDuplicateEntityCreated();
    }
}
