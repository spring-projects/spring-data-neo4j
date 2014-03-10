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

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.springframework.data.neo4j.template.Neo4jTemplateTests.Type.HAS;

public class Neo4jTemplateTests extends NeoApiTests {

    private Node refNode;

    enum Type implements RelationshipType {
        HAS
    }

    @Test
    public void testRefNode() {
        Node refNodeById = new Neo4jTemplate(graph, transactionManager).exec(new GraphCallback<Node>() {
            public Node doWithGraph(GraphDatabase graph) throws Exception {
                return graph.getNodeById( refNode.getId() );
            }
        });

        try (Transaction tx=graph.beginTx()) {
            assertEquals("same ref node", refNode, refNodeById);
            tx.success();
        }
    }

    @Test
    public void testSingleNode() {
        final Neo4jOperations template = new Neo4jTemplate(graph, transactionManager);
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                Node node = graph.createNode(map("name", "Test", "size", 100), null);
                refNode.createRelationshipTo(node, HAS);

                final Relationship toTestNode = refNode.getSingleRelationship(HAS, Direction.OUTGOING);
                final Node nodeByRelationship = toTestNode.getEndNode();
                assertEquals("Test", nodeByRelationship.getProperty("name"));
                assertEquals(100, nodeByRelationship.getProperty("size"));
            }
        });
        template.exec(new GraphCallback.WithoutResult() {
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                final Relationship toTestNode = refNode.getSingleRelationship(HAS, Direction.OUTGOING);
                final Node nodeByRelationship = toTestNode.getEndNode();
                assertEquals("Test", nodeByRelationship.getProperty("name"));
                assertEquals(100, nodeByRelationship.getProperty("size"));
            }
        });
    }

    @Test
    public void testRollback() {
        final Neo4jOperations template = new Neo4jTemplate(graph, transactionManager);
        try {
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                Node node = refNode;
                node.setProperty("test", "test");
                assertEquals("test", node.getProperty("test"));
                throw new RuntimeException();
            }
        });
        } catch(RuntimeException ignore) {}
        template.exec(new GraphCallback.WithoutResult() {
            public void doWithGraphWithoutResult(final GraphDatabase graph) throws Exception {
                assertFalse(refNode.hasProperty("test"));
            }
        });
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        refNode = new Neo4jTemplate(graph, transactionManager).exec(new GraphCallback<Node>() {
            public Node doWithGraph(GraphDatabase graph) throws Exception {
                return graph.createNode(map(), null);
            }
        });

    }
}
