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

package org.springframework.data.neo4j.template;

import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.Neo4jEmbeddedTransactionManager;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

public abstract class NeoApiTests {
    protected GraphDatabase graph;
    protected Neo4jTemplate template;
    protected PlatformTransactionManager transactionManager;
    private GraphDatabaseService graphDatabaseService;
    protected ConversionService conversionService;


    @Before
    public void setUp() throws Exception
    {
        conversionService = createConversionService();
        graph = createGraphDatabase();
        transactionManager = createTransactionManager();
        template = new Neo4jTemplate(graph, transactionManager);
    }

    private ConversionService createConversionService() throws Exception {
        return new Neo4jConversionServiceFactoryBean().getObject();
    }

    protected PlatformTransactionManager createTransactionManager()
    {
        return new JtaTransactionManager(new Neo4jEmbeddedTransactionManager(graphDatabaseService));
    }

    protected GraphDatabase createGraphDatabase() throws Exception
    {
        graphDatabaseService = new TestGraphDatabaseFactory().newImpermanentDatabase();
        return new DelegatingGraphDatabase(graphDatabaseService);
    }

    @After
    public void tearDown() {
        if (graphDatabaseService != null) {
            graphDatabaseService.shutdown();
        }
    }
}
