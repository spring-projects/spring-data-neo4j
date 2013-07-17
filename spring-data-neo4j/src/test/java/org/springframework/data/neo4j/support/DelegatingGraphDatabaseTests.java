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
package org.springframework.data.neo4j.support;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 11.04.12
 */
public class DelegatingGraphDatabaseTests {

    private DelegatingGraphDatabase graphDatabase;
    private GraphDatabaseService gdb;

    @Before
    public void setUp() throws Exception {
        gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        graphDatabase = new DelegatingGraphDatabase(gdb);
    }

    @After
    public void tearDown() throws Exception {
        graphDatabase.shutdown();
    }

    @Test
    public void testGetOrCreateNode() throws Exception {
        final Node node = graphDatabase.getOrCreateNode("user", "name", "David", map("name", "David"));
        final Node node2 = graphDatabase.getOrCreateNode("user", "name", "David", map("name", "David"));
        assertEquals("David",node.getProperty("name"));
        assertEquals(node,node2);
        assertEquals(node,gdb.index().forNodes("user").get("name","David").getSingle());
    }

    @Test
    public void testGetOrCreateRelationship() throws Exception {
        final Transaction tx = gdb.beginTx();
        final Node david = graphDatabase.createNode(map("name", "David"));
        final Node michael = graphDatabase.createNode(map("name", "Michael"));
        final Relationship rel1 = graphDatabase.getOrCreateRelationship("knows", "whom", "david_michael", david, michael, "KNOWS", map("whom", "david_michael"));
        final Relationship rel2 = graphDatabase.getOrCreateRelationship("knows", "whom", "david_michael", david, michael, "KNOWS", map("whom", "david_michael"));
        assertEquals("david_michael",rel1.getProperty("whom"));
        assertEquals("KNOWS",rel1.getType().name());
        assertEquals(david,rel1.getStartNode());
        assertEquals(michael,rel1.getEndNode());
        assertEquals(rel1,rel2);
        assertEquals(rel1,gdb.index().forRelationships("knows").get("whom","david_michael").getSingle());
        tx.success();
        tx.finish();
    }
}
