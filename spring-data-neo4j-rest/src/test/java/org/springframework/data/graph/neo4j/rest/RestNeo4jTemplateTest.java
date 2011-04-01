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

package org.springframework.data.graph.neo4j.rest;

import org.junit.BeforeClass;
import org.neo4j.rest.graphdb.RestTestHelper;
import org.springframework.data.graph.core.GraphDatabase;
import org.springframework.data.graph.neo4j.template.Neo4jTemplateTest;

public class RestNeo4jTemplateTest extends Neo4jTemplateTest
{
    private static RestTestHelper testHelper;

    @BeforeClass
    public static void startServer() throws Exception
    {
        testHelper = new RestTestHelper();
        testHelper.startServer();
    }

    @Override
    protected GraphDatabase createGraphDatabase() throws Exception
    {
        testHelper.cleanDb();
        return testHelper.createGraphDatabase();
    }
}
