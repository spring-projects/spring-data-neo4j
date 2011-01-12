package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mh
 * @since 12.01.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class GraphDatabaseContextTest {

    private static final String NAME = "name";
    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Autowired
    private FinderFactory finderFactory;
    private static final String VALUE = "test";

    @Test
    @Transactional
    public void testNodeIsIndexed() {
        Node node = graphDatabaseContext.createNode();
        node.setProperty(NAME, VALUE);
        Index<Node> nodeIndex = graphDatabaseContext.getNodeIndex(null);
        nodeIndex.add(node, NAME, VALUE);
        Assert.assertEquals("indexed node found", node, nodeIndex.get(NAME, VALUE).next());
    }

    @Test
    @Transactional
    public void testRelationshipIsIndexed() {
        Node node = graphDatabaseContext.createNode();
        Node node2 = graphDatabaseContext.createNode();
        Relationship indexedRelationship = node.createRelationshipTo(node2, DynamicRelationshipType.withName("relatesTo"));
        indexedRelationship.setProperty(NAME, VALUE);
        Index<Relationship> relationshipIndex = graphDatabaseContext.getRelationshipIndex(null);
        relationshipIndex.add(indexedRelationship, NAME, VALUE);
        Assert.assertEquals("indexed relationship found", indexedRelationship, relationshipIndex.get(NAME, VALUE).next());
    }

    @Before
    @Transactional
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

}