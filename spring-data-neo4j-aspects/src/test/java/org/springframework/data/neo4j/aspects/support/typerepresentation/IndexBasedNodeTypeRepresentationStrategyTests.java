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

package org.springframework.data.neo4j.aspects.support.typerepresentation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.support.typerepresentation.IndexBasedNodeTypeRepresentationStrategy;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests to ensure that all scenarios involved in entity creation / reading etc
 * behave as expected, specifically where the Index Based Type Representation Strategy
 * is being used.
 *
 * The common scenarios/tests are defined in the superclass and each subclass, which
 * represents a specific strategy, needs to ensure that all is when then they
 * are used
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/IndexingTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class IndexBasedNodeTypeRepresentationStrategyTests extends AbstractNodeTypeRepresentationStrategyTestBase {


    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        assertThat("The tests in this class should be configured to use the Index " +
                "based Type Representation Strategy, however it is not ... ",
                nodeTypeRepresentationStrategy,
                instanceOf(IndexBasedNodeTypeRepresentationStrategy.class));
    }

    @Test
	@Transactional
    @Override
	public void testPostEntityCreation() throws Exception {
		Index<Node> typesIndex = graphDatabaseService.index().forNodes(IndexBasedNodeTypeRepresentationStrategy.INDEX_NAME);

        // Things
        IndexHits<Node> thingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, thingType.getAlias());
		assertEquals(set(node(thing), node(subThing), node(subSubThing)), IteratorUtil.addToCollection((Iterable<Node>)thingHits, new HashSet<Node>()));

        // SubThings
        IndexHits<Node> subThingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, subThingType.getAlias());
        assertEquals(set(node(subThing), node(subSubThing)), IteratorUtil.addToCollection((Iterable<Node>)subThingHits, new HashSet<Node>()));

        // SubSubThings
        IndexHits<Node> subSubThingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, subSubThingType.getAlias());
        assertEquals(node(subSubThing), subSubThingHits.getSingle());

        // General
        assertEquals(thingType.getAlias(), node(thing).getProperty(IndexBasedNodeTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
		assertEquals(subSubThingType.getAlias(), node(subSubThing).getProperty(IndexBasedNodeTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
		thingHits.close();
		subSubThingHits.close();
	}

	@Test
    @Override
	public void testPreEntityRemoval() throws Exception {
        manualCleanDb();
        createThingsAndLinks();
        Index<Node> typesIndex;
        try (Transaction tx = graphDatabaseService.beginTx()) {
            typesIndex = graphDatabaseService.index().forNodes(IndexBasedNodeTypeRepresentationStrategy.INDEX_NAME);
            tx.success();
        }

        testPreEntityRemovalOfThing(typesIndex);
        testPreEntityRemovalOfSubThing(typesIndex);
        testPreEntityRemovalOfSubSubThing(typesIndex);
	}

    private void testPreEntityRemovalOfSubSubThing(Index<Node> typesIndex) {
        IndexHits<Node> thingHits;
        IndexHits<Node> subThingHits;
        IndexHits<Node> subSubThingHits;

        // 3. Remove SubSubThing
        try (Transaction tx = graphDatabaseService.beginTx()) {
            nodeTypeRepresentationStrategy.preEntityRemoval(node(subSubThing));
            tx.success();
        }

        try (Transaction tx = graphDatabaseService.beginTx()) {
            thingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, thingType.getAlias());
            assertNull(thingHits.getSingle());
            subThingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, subThingType.getAlias());
            assertNull(subThingHits.getSingle());
            subSubThingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, subSubThingType.getAlias());
            assertNull(subSubThingHits.getSingle());
            tx.success();
        }
    }
    private void testPreEntityRemovalOfSubThing(Index<Node> typesIndex) {
        IndexHits<Node> thingHits;
        IndexHits<Node> subThingHits;
        IndexHits<Node> subSubThingHits;

        // 1. Remove SubThing
        try (Transaction tx = graphDatabaseService.beginTx()) {
            nodeTypeRepresentationStrategy.preEntityRemoval(node(subThing));
            tx.success();
        }

        try (Transaction tx = graphDatabaseService.beginTx()) {
            thingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, thingType.getAlias());
            assertEquals(node(subSubThing), thingHits.getSingle());

            subThingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, subThingType.getAlias());
            assertEquals(node(subSubThing), subThingHits.getSingle());

            subSubThingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, subSubThingType.getAlias());
            assertEquals(node(subSubThing), subSubThingHits.getSingle());
            tx.success();
        }

    }

    private void testPreEntityRemovalOfThing(Index<Node> typesIndex) {
        IndexHits<Node> thingHits;
        IndexHits<Node> subThingHits;
        IndexHits<Node> subSubThingHits;

        // 1. Remove Thing
        try (Transaction tx = graphDatabaseService.beginTx()) {
            nodeTypeRepresentationStrategy.preEntityRemoval(node(thing));
            tx.success();
        }

        try (Transaction tx = graphDatabaseService.beginTx()) {
            thingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, thingType.getAlias());
            assertEquals(set(node(subThing), node(subSubThing)), IteratorUtil.addToCollection((Iterable<Node>)thingHits, new HashSet<Node>()));

            subThingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, subThingType.getAlias());
            assertEquals(set(node(subThing), node(subSubThing)), IteratorUtil.addToCollection((Iterable<Node>)subThingHits, new HashSet<Node>()));

            subSubThingHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, subSubThingType.getAlias());
            assertEquals(node(subSubThing), subSubThingHits.getSingle());
            tx.success();
        }

    }


}
