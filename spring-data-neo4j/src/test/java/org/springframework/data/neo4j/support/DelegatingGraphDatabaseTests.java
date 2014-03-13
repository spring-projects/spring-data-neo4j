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
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.data.neo4j.support.schema.SchemaIndexProvider;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.IteratorUtil.singleOrNull;
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
        try (Transaction tx = graphDatabase.beginTx()) {
            final Node node = graphDatabase.getOrCreateNode("user", "name", "David", map("name", "David"), null);
            final Node node2 = graphDatabase.getOrCreateNode("user", "name", "David", map("name", "David"), null);
            assertEquals("David",node.getProperty("name"));
            assertEquals(node,node2);
            assertEquals(node,gdb.index().forNodes("user").get("name","David").getSingle());
            tx.success();
        }
    }

    @Test
    public void mergeNode() throws Exception {
        try (Transaction tx = graphDatabase.beginTx()) {
            new SchemaIndexProvider(graphDatabase).createIndex("user","name",true);
            tx.success();
        }
        try (Transaction tx = graphDatabase.beginTx()) {
            final Node node = graphDatabase.merge("user", "name", "David", map("name", "David"), null);
            final Node node2 = graphDatabase.merge("user", "name", "David", map("name", "David"), null);
            assertEquals("David",node.getProperty("name"));
            assertEquals(node,node2);
            assertEquals(node,singleOrNull(gdb.findNodesByLabelAndProperty(DynamicLabel.label("user"), "name", "David")));
            tx.success();
        }
    }

    @Test
    public void mergeNodeWithLabel() throws Exception {
        try (Transaction tx = graphDatabase.beginTx()) {
            new SchemaIndexProvider(graphDatabase).createIndex("user","name",true);
            tx.success();
        }
        try (Transaction tx = graphDatabase.beginTx()) {
            final Node node = graphDatabase.merge("user", "name", "David", map("name", "David"), asList("person"));
            assertEquals("David",node.getProperty("name"));
            assertEquals(2, IteratorUtil.count(node.getLabels()));
            for (Label label : node.getLabels()) {
                assertEquals(true, asList("user", "person").contains(label.name()));
            }
            assertEquals(node,singleOrNull(gdb.findNodesByLabelAndProperty(DynamicLabel.label("user"), "name", "David")));
            tx.success();
        }
    }

    @Test
    public void testGetOrCreateRelationship() throws Exception {
        try (Transaction tx = graphDatabase.beginTx()) {
            final Node david = graphDatabase.createNode(map("name", "David"), asList("Person"));
            final Node michael = graphDatabase.createNode(map("name", "Michael"), asList("Person"));
            final Relationship rel1 = graphDatabase.getOrCreateRelationship("knows", "whom", "david_michael", david, michael, "KNOWS", map("whom", "david_michael"));
            final Relationship rel2 = graphDatabase.getOrCreateRelationship("knows", "whom", "david_michael", david, michael, "KNOWS", map("whom", "david_michael"));
            assertEquals("david_michael",rel1.getProperty("whom"));
            assertEquals("KNOWS",rel1.getType().name());
            assertEquals(david,rel1.getStartNode());
            assertEquals(michael,rel1.getEndNode());
            assertEquals(rel1,rel2);
            assertEquals(rel1,gdb.index().forRelationships("knows").get("whom","david_michael").getSingle());
            tx.success();
        }
    }
}
