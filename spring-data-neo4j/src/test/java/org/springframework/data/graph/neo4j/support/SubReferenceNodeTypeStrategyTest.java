package org.springframework.data.graph.neo4j.support;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * @author mh
 * @since 20.01.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class SubReferenceNodeTypeStrategyTest {

    @Autowired
    GraphDatabaseContext graphDatabaseContext;
    private NodeTypeStrategy nodeTypeStrategy;
    private Node thingNode;
    private Thing thing;

    @Before
    @Transactional
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
        nodeTypeStrategy = graphDatabaseContext.getNodeTypeStrategy();
        thingNode = createThing();
    }

    @Test
    @Transactional
    public void testPostEntityCreation() throws Exception {
        Node typeNode = getInstanceofRelationship().getOtherNode(thingNode);
        Assert.assertNotNull("type node for thing exists", typeNode);
        Assert.assertEquals("type node has property of type Thing.class", Thing.class.getName(), typeNode.getProperty(SubReferenceNodeTypeStrategy.SUBREF_CLASS_KEY));
        Assert.assertEquals("one thing has been created", 1, typeNode.getProperty(SubReferenceNodeTypeStrategy.SUBREFERENCE_NODE_COUNTER_KEY));
    }

    private Node createThing() {
        Node node = graphDatabaseContext.createNode();
        thing = new Thing(node);
        nodeTypeStrategy.postEntityCreation(thing);
        return node;
    }

    @Test
    @Transactional
    public void testPreEntityRemoval() throws Exception {
        Node typeNode = getInstanceofRelationship().getOtherNode(thingNode);
        nodeTypeStrategy.preEntityRemoval(thing);
        Assert.assertNull("instanceof relationship was removed", getInstanceofRelationship());
        Assert.assertEquals("no things left after removal", 0, typeNode.getProperty(SubReferenceNodeTypeStrategy.SUBREFERENCE_NODE_COUNTER_KEY));

    }

    @Transactional
    private Relationship getInstanceofRelationship() {
        return thingNode.getSingleRelationship(SubReferenceNodeTypeStrategy.INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
    }

    @Test
    @Transactional
    public void testCount() throws Exception {
        Assert.assertEquals("one thing created", 1, nodeTypeStrategy.count(Thing.class));
    }

    @Test
    @Transactional
    public void testGetJavaType() throws Exception {
        Assert.assertEquals("class in graph is thing", Thing.class, nodeTypeStrategy.<NodeBacked>getJavaType(thingNode));

    }

    @Test
    @Transactional
    public void testFindAll() throws Exception {
        Collection<Thing> things = IteratorUtil.asCollection(nodeTypeStrategy.findAll(Thing.class));
        Assert.assertEquals("one thing created and found", 1, things.size());
        Assert.assertTrue("result only contains Thing", things.iterator().next() instanceof Thing);
    }

    @NodeEntity
    public static class Thing {
        String name;

        public Thing() {
        }

        public Thing(Node n) {
            setUnderlyingState(n);
        }
    }
}
