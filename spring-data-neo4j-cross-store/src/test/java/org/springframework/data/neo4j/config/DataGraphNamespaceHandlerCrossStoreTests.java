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
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;

/**
 * @author mh
 * @since 31.01.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:org/springframework/data/neo4j/config/DataGraphNamespaceHandlerTest-cross-store-context.xml")
public class DataGraphNamespaceHandlerCrossStoreTests {

    @Autowired GraphDatabaseService graphDatabaseService;
    @Autowired Neo4jTemplate template;
    @Autowired PlatformTransactionManager transactionManager;

    @Test public void injectionForCrossStore() {
        Assert.assertNotNull("template", template);
        GraphDatabaseService graphDatabaseService = template.getGraphDatabaseService();
        String fileSeparator = "target" + System.getProperty("file.separator") + "config-test";
//        Assert.assertTrue("store-dir", graphDatabaseService.getStoreDir().endsWith(fileSeparator));
        Assert.assertNotNull("graphDatabaseService", graphDatabaseService);
        Assert.assertNotNull("transactionManager", transactionManager);
    }
}
