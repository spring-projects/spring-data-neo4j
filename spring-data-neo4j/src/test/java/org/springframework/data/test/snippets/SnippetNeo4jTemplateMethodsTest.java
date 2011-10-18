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
package org.springframework.data.test.snippets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.test.DocumentingTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:DocumentingTest-context.xml"})
public class SnippetNeo4jTemplateMethodsTest extends DocumentingTestBase {
    @Autowired
    private GraphDatabase graphDatabase;
    private static final RelationshipType WORKS_WITH = DynamicRelationshipType.withName("WORKS_WITH");

    @Test
    @Transactional
    public void documentTemplateMethods() {
        title ="Basic operations";
        paragraphs = new String[] {"For direct retrieval of nodes and relationships, the <code>getReferenceNode()</code>,\n" +
                "           <code>getNode()</code> and <code>getRelationship()</code> methods can be used.",
                "There are methods (<code>createNode()</code> and <code>createRelationship()</code>) for creating nodes and\n" +
                        "           relationships that automatically set provided properties."};

        snippetTitle = "Neo4j template";
        snippet = "template";

        // SNIPPET template
        // TODO auto-post-construct !!
        Neo4jOperations neo = new Neo4jTemplate(graphDatabase).postConstruct();

        Node mark = neo.createNode(map("name", "Mark"));
        Node thomas = neo.createNode(map("name", "Thomas"));

        neo.createRelationshipBetween(mark, thomas, "WORKS_WITH", map("project", "spring-data"));

        neo.index("devs", thomas, "name", "Thomas");
        // Cypher TODO
        assertEquals( "Mark", neo.query("start p=node({person}) match p<-[:WORKS_WITH]-other return other.name",
                                  map("person", asList(thomas.getId()))).to(String.class).single());


        // SNIPPET template

        String thisShouldNotBePartOfTheSnippet;

        // SNIPPET template

        // Gremlin
        assertEquals(thomas, neo.execute("g.v(person).out('WORKS_WITH')",
                map("person", mark.getId())).to(Node.class).single());

        // Index lookup
        assertEquals(thomas, neo.lookup("devs", "name", "Thomas").to(Node.class).single());

        // Index lookup with Result Converter
        assertEquals("Thomas", neo.lookup("devs", "name", "Thomas").to(String.class, new ResultConverter<PropertyContainer, String>() {
            public String convert(PropertyContainer element, Class<String> type) {
                return (String) element.getProperty("name");
            }
        }).single());
        // SNIPPET template
    }

}
