package org.springframework.datastore.graph.neo4j.template;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.StopEvaluator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.datastore.graph.neo4j.template.graph.GraphDescription;
import org.springframework.datastore.graph.neo4j.template.traversal.Traversal;

import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.springframework.datastore.graph.neo4j.template.NeoTraversalTest.Type.HAS;
import static org.springframework.datastore.graph.neo4j.template.traversal.Traversal.walk;

public class NeoTraversalTest extends NeoApiTest {

    enum Type implements RelationshipType {
        MARRIED, CHILD, GRANDSON, GRANDDAUGHTER, WIFE, HUSBAND, HAS
    }

    @Test
    public void testSimpleTraverse() {
        runAndCheckTraverse(walk().both(HAS), "wife", "man","son","daughter", "grandma","grandpa" );
    }


    @Ignore
    @Test
    public void testComplexTraversal() {
        final Traversal traversal = walk().breadthFirst().depthFirst()
                .stopOn(StopEvaluator.DEPTH_ONE).first().all()
                .incoming(HAS).outgoing(HAS).twoway(HAS);
        runAndCheckTraverse(traversal, "grandpa", "grandma", "daughter", "son", "man", "wife");
    }

    private void runAndCheckTraverse(final Traversal traversal, final String... names) {
        final NeoTemplate template = new NeoTemplate(neo);
        template.execute(new NeoCallback() {
            public void neo(final Status status, final Graph graph) throws Exception {
                createFamily(graph);
                assertEquals("all members",asList(names) ,
                        graph.traverse(graph.getReferenceNode(), traversal,
                                new Converter<Node, String>() {
                                    public String convert(Node node) {
                                        return (String) node.getProperty("name", "");
                                    }
                                }));
            }
        });
    }

    private void createFamily(final Graph graph) {
        final GraphDescription family = new GraphDescription();
        family.add("family", "type", "small");
        family.relate("family", HAS, "wife");
        family.relate("family", HAS, "man");


        family.add("man", "age", 35);
        family.add("wife", "age", 30);
        family.relate("man", Type.MARRIED, "wife");
        family.relate("wife", Type.MARRIED, "man");
        family.relate("man", Type.WIFE, "wife");
        family.relate("wife", Type.HUSBAND, "man");

        family.add("daughter", "age", 10);
        family.add("son", "age", 8);

        family.relate("family", HAS, "son");
        family.relate("family", HAS, "daughter");

        family.relate("man", Type.CHILD, "son");
        family.relate("wife", Type.CHILD, "son");
        family.relate("man", Type.CHILD, "daughter");
        family.relate("wife", Type.CHILD, "daughter");

        family.add("grandma", "age", 60);
        family.add("grandpa", "age", 75);

        family.relate("family", HAS, "grandma");
        family.relate("family", HAS, "grandpa");

        family.relate("grandpa", Type.CHILD, "man");
        family.relate("grandma", Type.CHILD, "man");

        family.relate("grandpa", Type.GRANDSON, "son");
        family.relate("grandma", Type.GRANDSON, "son");
        family.relate("grandpa", Type.GRANDDAUGHTER, "daughter");
        family.relate("grandma", Type.GRANDDAUGHTER, "daughter");

        // todo even more relationships
        graph.load(family);
    }
}