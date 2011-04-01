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
import org.neo4j.graphdb.*;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public abstract class NeoApiTest {
    protected GraphDatabaseService graph;
    protected Neo4jTemplate template;


    @Before
    public void setUp() {
        graph = new EmbeddedGraphDatabase("target/template-db");
        template = new Neo4jTemplate(graph);
    }

    @After
    public void tearDown() {
        if (graph != null) {
            clear();
            graph.shutdown();
        }
    }

    private void clear() {
        try {
            template.exec(new GraphCallback<Void>() {
                public Void doWithGraph(GraphDatabaseService graph) throws Exception {
                    for (Node node : graph.getAllNodes()) {
                        for (Relationship relationship : node.getRelationships()) {
                            relationship.delete();
                        }
                    }
                    Node referenceNode = graph.getReferenceNode();
                    for (Node node : graph.getAllNodes()) {
                        if (node.equals(referenceNode)) continue;
                        node.delete();
                    }
                    return null;
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
            // ignore
        }
    }
}
