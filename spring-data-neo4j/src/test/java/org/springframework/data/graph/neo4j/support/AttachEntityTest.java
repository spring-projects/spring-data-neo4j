package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.Developer;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

public class AttachEntityTest {

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DirectGraphRepositoryFactory graphRepositoryFactory;

    @Test
    @Transactional
    public void entityShouldHaveNoNode() {
        Developer dev = new Developer("Michael");
        assertFalse(hasPersistentState(dev));
        assertNull(nodeFor(dev));
    }

    private boolean hasPersistentState(NodeBacked nodeBacked) {
        return nodeBacked.hasPersistentState();
    }

    private Node nodeFor(NodeBacked nodeBacked) {
        return nodeBacked.getPersistentState();
    }


}
