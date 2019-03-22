/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.rest.support.RestTestHelper;
import org.springframework.data.neo4j.template.Neo4jTemplateTests;
import org.springframework.transaction.PlatformTransactionManager;

public class RestNeo4jTemplateTests extends Neo4jTemplateTests
{
    private static RestTestHelper testHelper;

    @BeforeClass
    public static void startServer() throws Exception
    {
        testHelper = new RestTestHelper();
        testHelper.startServer();
    }

    @AfterClass
    public static void stopServer() throws Exception
    {
        testHelper.shutdownServer();
    }

    @Override
    protected PlatformTransactionManager createTransactionManager()
    {
        return null;
    }

    @Override
    protected GraphDatabase createGraphDatabase() throws Exception
    {
        testHelper.cleanDb();
        final GraphDatabase graphDatabase = testHelper.createGraphDatabase();
        graphDatabase.setConversionService(conversionService);
        return graphDatabase;
    }

    @Override
    public void testRollback()
    {
    }
}
