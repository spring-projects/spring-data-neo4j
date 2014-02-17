/**
 * Copyright 2014 the original author or authors.
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

package org.springframework.data.neo4j.aspects.support;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.junit.Assert.assertTrue;

/**
 * This version still DOES NOT work, tried getting the initialEntity set to be populated
 * based on base-package scanning .
 *
 * @author Nicki Watt
 * @since 09-02-2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:org/springframework/data/neo4j/aspects/support/LabelBasedIndexedPropertyEntityTests-context-basic.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/LabelingTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class})
public class LabelBasedIndexedPropertyHangingWithBasicTests extends  LabelBasedIndexedPropertyEntityTestBase {

    @Override
    @Before
    public void cleanDb() {
        assertTrue("This test expects a Label Based TRS to be in place and it is not!",neo4jTemplate.isLabelBased());
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Neo4jHelper.cleanDb(neo4jTemplate);
            tx.success();
        }
        queryEngine = neo4jTemplate.queryEngineFor(QueryType.Cypher);

        // NW-ISSUE01
        // Still doesn't work (initialEntitySet not being populated in mappingContext)
        // see LabelBasedIndexedPropertyEntityTests-context-basic.xml is doing a component
        // scan but not picked up??
        try (Transaction tx = graphDatabaseService.beginTx()) {
            createThing();
            createSubThing();
            createSubSubThing();
            tx.success();
        }
    }

}
