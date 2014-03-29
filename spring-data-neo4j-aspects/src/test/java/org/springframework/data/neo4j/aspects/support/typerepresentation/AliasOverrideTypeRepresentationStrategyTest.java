package org.springframework.data.neo4j.aspects.support.typerepresentation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.aspects.support.domain.*;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 * @author Nicki Watt
 * @since 15.04.2014
 */
public class AliasOverrideTypeRepresentationStrategyTest {

    protected GraphDatabaseService graphDatabaseService;
    protected Neo4jTemplate neo4jTemplate;
    protected ClassPathXmlApplicationContext applicationContext;

    private void setupDefaultAppCtx() {
        applicationContext = new ClassPathXmlApplicationContext(
                "org/springframework/data/neo4j/aspects/support/LabelBasedIndexedPropertyEntityTests-context-basic.xml");
        applicationContext.registerShutdownHook();
        applicationContext.refresh();
        graphDatabaseService = applicationContext.getBean(GraphDatabaseService.class);
        neo4jTemplate = applicationContext.getBean(Neo4jTemplate.class);
    }

    private void setupOverrideAppCtx() {
        applicationContext = new ClassPathXmlApplicationContext(
                "org/springframework/data/neo4j/aspects/support/classNameAliasOverride-context.xml");
        applicationContext.registerShutdownHook();
        applicationContext.refresh();
        graphDatabaseService = applicationContext.getBean(GraphDatabaseService.class);
        neo4jTemplate = applicationContext.getBean(Neo4jTemplate.class);
    }

    @After
    public void tearDown() {
       if (applicationContext != null) applicationContext.stop();
    }

    @Test
    public void testSimpleNameUsedForDefaultTRS() {
        setupDefaultAppCtx();
        String simpleLabelName = Thing.class.getSimpleName();
        testExpectedNameAppliedForLabelTRS(simpleLabelName);
    }

    @Test
    public void testFQDNNameUsedForOverridenTRS() {
        setupOverrideAppCtx();
        String fqdnLabelName = Thing.class.getName();
        testExpectedNameAppliedForLabelTRS(fqdnLabelName);
    }

    public void testExpectedNameAppliedForLabelTRS(String expectedLabelName) {
        // Given I create a Node Entity and it is saved into the graph
        // And I verify this by ensuring the nodeId is present
        Thing thing = createThing(graphDatabaseService);
        Long savedNodeId = ((NodeBacked) thing).getNodeId();
        assertNotNull("Node Id Should have been assigned as part of persist call ",savedNodeId);

        // When I get all the labels from this Node
        // Then I expect to find the two appropriate TRS based labels
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node retrievedNode = neo4jTemplate.getNode(savedNodeId);
            Collection<Label> labels = IteratorUtil.asCollection(retrievedNode.getLabels());
            assertThat( labels, hasItems(
                    DynamicLabel.label(expectedLabelName),
                    DynamicLabel.label("_" + expectedLabelName)));
        }

    }

    private Thing createThing(GraphDatabaseService graphDB) {
        Thing thing = null;
        try (Transaction tx = graphDB.beginTx()) {
            thing = new Thing();
            thing.setName("theThing");
            ((NodeBacked)thing).persist();
            tx.success();
        }
        return thing;
    }

}
