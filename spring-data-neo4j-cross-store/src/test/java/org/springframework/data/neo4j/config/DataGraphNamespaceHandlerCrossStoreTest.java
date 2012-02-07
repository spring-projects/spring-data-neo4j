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

package org.springframework.data.neo4j.config;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author mh
 * @since 31.01.11
 */
public class DataGraphNamespaceHandlerCrossStoreTest {

    static class Config {
        @Autowired
        GraphDatabaseService graphDatabaseService;
        @Autowired
        Neo4jTemplate template;
        @Autowired
        PlatformTransactionManager transactionManager;
    }

    @Test
    public void injectionForCrossStore() {
        assertInjected("-cross-store");
    }

    private Config assertInjected(String testCase) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:org/springframework/data/neo4j/config/DataGraphNamespaceHandlerTest" + testCase + "-context.xml");
        Config config = ctx.getBean("config", Config.class);
        Neo4jTemplate template = config.template;
        Assert.assertNotNull("template", template);
        EmbeddedGraphDatabase graphDatabaseService = (EmbeddedGraphDatabase) template.getGraphDatabaseService();
        String fileSeparator = "target" + System.getProperty("file.separator") + "config-test";
        Assert.assertTrue("store-dir", graphDatabaseService.getStoreDir().endsWith(fileSeparator));
        Assert.assertNotNull("graphDatabaseService", config.graphDatabaseService);
        Assert.assertNotNull("transactionManager", config.transactionManager);
        config.graphDatabaseService.shutdown();
        return config;
    }

}
