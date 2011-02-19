package org.springframework.data.graph.neo4j.template;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.Traversal;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.springframework.data.graph.neo4j.template.NeoTraversalTest.Type.HAS;

public class NeoTraversalTest extends NeoApiTest {

    enum Type implements RelationshipType {
        MARRIED, CHILD, GRANDSON, GRANDDAUGHTER, WIFE, HUSBAND, HAS
    }

    @Test
    public void testSimpleTraverse() {
        runAndCheckTraverse(Traversal.description().filter(Traversal.returnAllButStartNode()).relationships(HAS),  "grandpa", "grandma","daughter","son","man","wife" );
    }


    @Ignore
    @Test
    public void testComplexTraversal() {
        final TraversalDescription traversal = Traversal.description().relationships(HAS).prune(Traversal.pruneAfterDepth(1));
        runAndCheckTraverse(traversal, "grandpa", "grandma", "daughter", "son", "man", "wife");
    }

    private void runAndCheckTraverse(final TraversalDescription traversal, final String... names) {
        final Neo4jOperations template = new Neo4jTemplate(graph);
        template.doInTransaction(new GraphTransactionCallback<Void>() {
            public Void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                createFamily(graph);
                return null;
            }});
        Iterable<String> result=template.traverse(template.getReferenceNode(), traversal,
                new PathMapper<String>() {
                    @Override
                    public String mapPath(Path path) {
                        return (String) path.endNode().getProperty("name", "");
                    }
                });
        assertEquals("all members", asList(names), IteratorUtil.<String>asCollection(result));
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