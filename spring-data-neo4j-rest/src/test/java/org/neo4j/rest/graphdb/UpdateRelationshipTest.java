package org.neo4j.rest.graphdb;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.rest.graphdb.query.CypherResult;
import org.neo4j.rest.graphdb.query.CypherTransactionExecutionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.IteratorUtil.count;
import static org.neo4j.helpers.collection.MapUtil.map;

public class UpdateRelationshipTest extends RestTestBase {

    public static final DynamicRelationshipType KNOWS = DynamicRelationshipType.withName("KNOWS");
    public static final Label FRIEND = DynamicLabel.label("Friend");
    private DynamicRelationshipType type = KNOWS;
    public static final DynamicRelationshipType LIKES = DynamicRelationshipType.withName("LIKES");
    private Direction direction = Direction.OUTGOING;
    private Node remove;
    private Node keep;
    private Node add;
    protected RestAPI restAPI;
    private List<Node> expected;
    private List<Node> updateTo;
    private String targetLabel = null;

    @Override
    protected GraphDatabaseService createRestGraphDatabase() {
        restAPI = new RestAPICypherImpl(new RestAPIImpl(SERVER_ROOT_URI));
        return new CypherRestGraphDatabase(restAPI);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        GraphDatabaseService db = getRestGraphDb();
        remove = db.createNode();
        keep = db.createNode();
        add = db.createNode();
    }

    @Test
    public void testUpdateRelationshipsRemoveAddNoDirection() throws Exception {
        Node node = node();
        remove.createRelationshipTo(node, KNOWS);
        node.createRelationshipTo(keep, KNOWS);
        direction = null;
        updateTo = asList(keep,add);
        updateRelationships();
    }

    @Test
    public void testUpdateRelationshipsRemoveAddBothDirection() throws Exception {
        Node node = node();
        remove.createRelationshipTo(node, KNOWS);
        node.createRelationshipTo(keep, KNOWS);
        direction = Direction.BOTH;
        updateTo = asList(keep,add);
        updateRelationships();
    }

    /*
    given
                   (remove)-[:LIKES]->(node)
                   (node)-[:KNOWS]->(keep)
    when update to (node)-->(keep)
    then
     */
    @Test(expected = CypherTransactionExecutionException.class)
    public void testUpdateRelationshipsRemoveAddNoType() throws Exception {
        Node node = node();
        remove.createRelationshipTo(node, LIKES);
        node.createRelationshipTo(keep, KNOWS);
        type = null;
        updateTo = asList(keep);
        expected = asList(remove, keep);
        updateRelationships();
    }

    @Test
    public void testUpdateRelationshipsRemoveAdd() throws Exception {
        Node node = node();
        node.createRelationshipTo(remove, KNOWS);
        node.createRelationshipTo(keep, KNOWS);
        updateTo = asList(keep, add);
        updateRelationships();
    }

    @Test
    public void testUpdateRelationshipsRemoveAddKeepOtherType() throws Exception {
        Node node = node();
        node.createRelationshipTo(remove, LIKES);
        node.createRelationshipTo(remove, KNOWS);
        node.createRelationshipTo(keep, KNOWS);
        updateTo = asList(keep, add);
        expected = asList(remove, keep, add);
        updateRelationships();
    }
    @Test
    public void testUpdateRelationshipsRemoveAddKeepOtherDirection() throws Exception {
        Node node = node();
        remove.createRelationshipTo(node, KNOWS);
        node.createRelationshipTo(remove, KNOWS);
        node.createRelationshipTo(keep, KNOWS);
        updateTo = asList(keep, add);
        expected = asList(remove, keep, add);
        updateRelationships();
    }
    @Test
    public void testUpdateRelationshipsRemoveAddKeepOtherLabel() throws Exception {
        Node node = node();
        remove.addLabel(FRIEND);
        node.createRelationshipTo(remove, KNOWS);
        node.createRelationshipTo(keep, KNOWS);
        updateTo = asList();
        expected = asList(keep);
        targetLabel = FRIEND.name();
        updateRelationships();
    }

    @Test
    public void testUpdateRelationshipNothing() throws Exception {
        updateTo = asList();
        updateRelationships();
    }
    @Test
    public void testUpdateRelationshipsAdd() throws Exception {
        updateTo = asList(add);
        updateRelationships();
    }

    @Test
    public void testUpdateRelationshipsRemove() throws Exception {
        Node node = node();
        node.createRelationshipTo(remove, KNOWS);
        updateTo = asList();
        updateRelationships();
    }

    protected void updateRelationships() {
        if (expected==null) expected = updateTo;
        Iterable<Relationship> rels = restAPI.updateRelationships(node(), updateTo, type, direction, targetLabel);
        CypherResult result = restAPI.query("MATCH (n)--(m) WHERE id(n) = {id} WITH id(m) as id ORDER BY id RETURN collect(id) as ids", map("id", node().getId()));
        assertEquals(1, count(result.getData()));
        assertEquals(toSortedIdList(expected), result.getData().iterator().next().get(0));
    }

    private List<Integer> toSortedIdList(List<Node> endNodes) {
        List<Integer> expected = new ArrayList<>(endNodes.size());
        for (Node node : endNodes) expected.add((int) node.getId());
        Collections.sort(expected);
        return expected;
    }
}
