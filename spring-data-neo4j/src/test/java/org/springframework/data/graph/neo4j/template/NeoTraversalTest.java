package org.springframework.data.graph.neo4j.template;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.kernel.Traversal;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.kernel.Traversal.returnAllButStartNode;
import static org.springframework.data.graph.neo4j.template.NeoTraversalTest.Type.HAS;
import static org.springframework.data.graph.neo4j.template.PropertyMap._;

public class NeoTraversalTest extends NeoApiTest {

    enum Type implements RelationshipType {
        MARRIED, CHILD, GRANDSON, GRANDDAUGHTER, WIFE, HUSBAND, HAS
    }

    @Test
    public void testSimpleTraverse() {
        template.exec(new GraphCallback<Void>() {
            public Void doWithGraph(GraphDatabaseService graph) throws Exception {
                createFamily();
                return null;
            }
        });

        final Set<String> resultSet = new HashSet<String>();
        template.traverseGraph(template.getReferenceNode(), new PathMapper.WithoutResult() {
            @Override
            public void eachPath(Path path) {
                String nodeName = (String) path.endNode().getProperty("name", "");
                resultSet.add(nodeName);
            }
        }, Traversal.description().relationships(HAS).filter(returnAllButStartNode()).prune(Traversal.pruneAfterDepth(2)));
        assertEquals("all members", new HashSet<String>(asList("grandpa", "grandma", "daughter", "son", "man", "wife", "family")), resultSet);
    }


    private void createFamily() {

        Node family = template.createNode(_("name", "family"));
        Node man = template.createNode(_("name", "wife"));
        Node wife = template.createNode(_("name", "man"));
        family.createRelationshipTo(man, HAS);
        family.createRelationshipTo(wife, HAS);

        Node daughter = template.createNode(_("name", "daughter"));
        family.createRelationshipTo(daughter, HAS);
        Node son = template.createNode(_("name", "son"));
        family.createRelationshipTo(son, HAS);
        man.createRelationshipTo(son, Type.CHILD);
        wife.createRelationshipTo(son, Type.CHILD);
        man.createRelationshipTo(daughter, Type.CHILD);
        wife.createRelationshipTo(daughter, Type.CHILD);

        Node grandma = template.createNode(_("name", "grandma"));
        Node grandpa = template.createNode(_("name", "grandpa"));

        family.createRelationshipTo(grandma, HAS);
        family.createRelationshipTo(grandpa, HAS);

        grandma.createRelationshipTo(man, Type.CHILD);
        grandpa.createRelationshipTo(man, Type.CHILD);

        grandma.createRelationshipTo(son, Type.GRANDSON);
        grandpa.createRelationshipTo(son, Type.GRANDSON);
        grandma.createRelationshipTo(daughter, Type.GRANDDAUGHTER);
        grandpa.createRelationshipTo(daughter, Type.GRANDDAUGHTER);

        graph.getReferenceNode().createRelationshipTo(family,HAS);
    }
}