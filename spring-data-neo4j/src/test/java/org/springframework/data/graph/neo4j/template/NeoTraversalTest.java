package org.springframework.data.graph.neo4j.template;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.Traversal;
import org.springframework.core.convert.converter.Converter;

import java.util.Iterator;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.springframework.data.graph.neo4j.template.NeoTraversalTest.Type.HAS;

public class NeoTraversalTest extends NeoApiTest {

    enum Type implements RelationshipType {
        MARRIED, CHILD, GRANDSON, GRANDDAUGHTER, WIFE, HUSBAND, HAS
    }

    @Test
    public void testSimpleTraverse() {
        runAndCheckTraverse(Traversal.description().depthFirst().relationships(HAS), "wife", "man","son","daughter", "grandma","grandpa" );
    }


    @Ignore
    @Test
    public void testComplexTraversal() {
        final TraversalDescription traversal = Traversal.description().depthFirst().relationships(HAS).prune(Traversal.pruneAfterDepth(1));
        runAndCheckTraverse(traversal, "grandpa", "grandma", "daughter", "son", "man", "wife");
    }

    private void runAndCheckTraverse(final TraversalDescription traversal, final String... names) {
        final Neo4jTemplate template = new Neo4jTemplate(neo);
        template.doInTransaction(new TransactionGraphCallback() {
            public void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                createFamily(graph);
            }});
        Iterator<String> result=template.traverseNodes(template.getReferenceNode(), traversal,
                new Converter<Node, String>() {
                    public String convert(Node node) {
                        return (String) node.getProperty("name", "");
                    }
                });
        assertEquals("all members", asList(names), IteratorUtil.<String>asCollection(IteratorUtil.asIterable(result)));
    }

    private void createFamily(final GraphDatabaseService graph) {
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

        family.addToGraph(graph);
    }
}