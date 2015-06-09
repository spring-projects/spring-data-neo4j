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

package org.springframework.data.neo4j.rest.support;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.aspects.support.query.QueryEngineTests;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.rest.SpringCypherRestGraphDatabase;
import org.springframework.data.neo4j.rest.SpringCypherRestGraphDatabase;
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * @author mh
 * @since 23.06.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
    "classpath:RestTests-context-index.xml"})
public class RestQueryEngineTests extends QueryEngineTests {

    @Autowired
    SpringRestGraphDatabase restGraphDatabase;

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

    @Test
    @Ignore
    public void testQueryNumericallyIndexedPropertiesWithNumericRangeQuery() {
    }

    @Override
    protected GraphDatabase createGraphDatabase() throws Exception {
        restGraphDatabase.setConversionService(conversionService);
        return restGraphDatabase;
    }
}
