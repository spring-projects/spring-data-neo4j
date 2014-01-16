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

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.springframework.data.neo4j.conversion.Handler;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.ReferenceNodes;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.neo4j.helpers.collection.MapUtil.map;
// TODO import static org.neo4j.kernel.Traversal.returnAllButStartNode;
import static org.springframework.data.neo4j.template.NeoTraversalTests.Type.HAS;

public class NeoTraversalTests extends NeoApiTests {

    enum Type implements RelationshipType {
        MARRIED, CHILD, GRANDSON, GRANDDAUGHTER, WIFE, HUSBAND, HAS
    }

    @Test
    public void testSimpleTraverse() {
        final Node family = template.exec(new GraphCallback<Node>() {
            @Override
            public Node doWithGraph(GraphDatabase graph) throws Exception {
                return createFamily();
            }
        });

        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
        final Set<String> resultSet = new HashSet<String>();
            final TraversalDescription description = Traversal.description().relationships(HAS).evaluator(Evaluators.excludeStartPosition()).evaluator(Evaluators.toDepth(1));

            final Result<Path> result = template.traverse(family, description);
            result.handle(new Handler<Path>() {
                @Override
                public void handle(Path value) {
                    final String name = (String) value.endNode().getProperty("name", "");
                    resultSet.add(name);
                        }
                });
        assertEquals("all members", new HashSet<String>(asList("grandpa", "grandma", "daughter", "son", "man", "wife")), resultSet);
            }
        });
    }


    private Node createFamily() {

        Node family = template.createNode(map("name", "family"));
        Node man = template.createNode(map("name", "wife"));
        Node wife = template.createNode(map("name", "man"));
        family.createRelationshipTo(man, HAS);
        family.createRelationshipTo(wife, HAS);

        Node daughter = template.createNode(map("name", "daughter"));
        family.createRelationshipTo(daughter, HAS);
        Node son = template.createNode(map("name", "son"));
        family.createRelationshipTo(son, HAS);
        man.createRelationshipTo(son, Type.CHILD);
        wife.createRelationshipTo(son, Type.CHILD);
        man.createRelationshipTo(daughter, Type.CHILD);
        wife.createRelationshipTo(daughter, Type.CHILD);

        Node grandma = template.createNode(map("name", "grandma"));
        Node grandpa = template.createNode(map("name", "grandpa"));

        family.createRelationshipTo(grandma, HAS);
        family.createRelationshipTo(grandpa, HAS);

        grandma.createRelationshipTo(man, Type.CHILD);
        grandpa.createRelationshipTo(man, Type.CHILD);

        grandma.createRelationshipTo(son, Type.GRANDSON);
        grandpa.createRelationshipTo(son, Type.GRANDSON);
        grandma.createRelationshipTo(daughter, Type.GRANDDAUGHTER);
        grandpa.createRelationshipTo(daughter, Type.GRANDDAUGHTER);

        return family;
    }
}