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

package org.springframework.data.graph.neo4j.template;

import org.junit.After;
import org.junit.Before;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.ImpermanentGraphDatabase;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.springframework.data.graph.core.GraphDatabase;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;
import org.springframework.transaction.jta.JtaTransactionManager;

public abstract class NeoApiTest {
    protected GraphDatabase graph;
    protected Neo4jTemplate template;
    protected JtaTransactionManager transactionManager;
    private AbstractGraphDatabase graphDatabaseService;


    @Before
    public void setUp() throws Exception
    {
        graph = createGraphDatabase();
        transactionManager = new JtaTransactionManager(new SpringTransactionManager(graphDatabaseService));
        template = new Neo4jTemplate(graph, transactionManager);
    }

    protected GraphDatabase createGraphDatabase() throws Exception
    {
        graphDatabaseService = new ImpermanentGraphDatabase( );
        return new DelegatingGraphDatabase(graphDatabaseService);
    }

    @After
    public void tearDown() {
        if (graphDatabaseService != null) {
            graphDatabaseService.shutdown();
        }
    }
}
