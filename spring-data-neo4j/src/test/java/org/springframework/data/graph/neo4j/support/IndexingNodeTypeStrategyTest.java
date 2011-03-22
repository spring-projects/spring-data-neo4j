package org.springframework.data.graph.neo4j.support;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml",
		"classpath:org/springframework/data/graph/neo4j/support/IndexingNodeTypeStrategyOverride-context.xml"})
public class IndexingNodeTypeStrategyTest {

	@Autowired
	private GraphDatabaseService graphDatabaseService;
	@Autowired
	private IndexingNodeTypeStrategy nodeTypeStrategy;

	private Thing thing;
	private SubThing subThing;

	@BeforeTransaction
	public void cleanDb() {
		Neo4jHelper.cleanDb(graphDatabaseService);
	}

	@Before
	public void setUp() throws Exception {
		if (thing == null) {
			createThings();
		}
	}

	@Test
	@Transactional
	public void testPostEntityCreation() throws Exception {
		Index<Node> typesIndex = graphDatabaseService.index().forNodes("__types__");
		IndexHits<Node> thingHits = typesIndex.get("className", thing.getClass().getName());
		assertEquals(set(node(thing), node(subThing)), IteratorUtil.addToCollection((Iterable<Node>)thingHits, new HashSet<Node>()));
		IndexHits<Node> subThingHits = typesIndex.get("className", subThing.getClass().getName());
		assertEquals(node(subThing), subThingHits.getSingle());
		assertEquals(thing.getClass().getName(), node(thing).getProperty("__type__"));
		assertEquals(subThing.getClass().getName(), node(subThing).getProperty("__type__"));
	}

	@Test
	public void testPreEntityRemoval() throws Exception {
        manualCleanDb();
        createThings();
		Index<Node> typesIndex = graphDatabaseService.index().forNodes("__types__");
		IndexHits<Node> thingHits;
		IndexHits<Node> subThingHits;

        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            nodeTypeStrategy.preEntityRemoval(thing);
            tx.success();
        }
        finally
        {
            tx.finish();
        }

		thingHits = typesIndex.get("className", thing.getClass().getName());
		assertEquals(node(subThing), thingHits.getSingle());
		subThingHits = typesIndex.get("className", subThing.getClass().getName());
		assertEquals(node(subThing), subThingHits.getSingle());

        tx = graphDatabaseService.beginTx();
        try
        {
            nodeTypeStrategy.preEntityRemoval(subThing);
            tx.success();
        }
        finally
        {
            tx.finish();
        }

		thingHits = typesIndex.get("className", thing.getClass().getName());
        assertNull(thingHits.getSingle());
		subThingHits = typesIndex.get("className", subThing.getClass().getName());
        assertNull(subThingHits.getSingle());
	}

	@Test
	@Transactional
	public void testFindAll() throws Exception {
		assertEquals("Did not find all things.",
				Arrays.asList(thing, subThing),
				IteratorUtil.addToCollection(nodeTypeStrategy.findAll(Thing.class), new ArrayList<Thing>()));
	}

	@Test
	@Transactional
	public void testCount() throws Exception {
		assertEquals(2, nodeTypeStrategy.count(Thing.class));
	}

	@Test
	@Transactional
	public void testGetJavaType() throws Exception {
		assertEquals(Thing.class, nodeTypeStrategy.getJavaType(node(thing)));
		assertEquals(SubThing.class, nodeTypeStrategy.getJavaType(node(subThing)));
	}

	@Test
	@Transactional
	public void testConfirmType() throws Exception {
		assertEquals(Thing.class, nodeTypeStrategy.confirmType(node(thing), Thing.class));
		assertEquals(SubThing.class, nodeTypeStrategy.confirmType(node(subThing), Thing.class));
	}

	private static Node node(Thing thing) {
		return thing.getPersistentState();
	}

	private Thing createThings() {
		Transaction tx = graphDatabaseService.beginTx();
		try {
			thing = new Thing(graphDatabaseService.createNode());
			nodeTypeStrategy.postEntityCreation(thing);
	        subThing = new SubThing(graphDatabaseService.createNode());
			nodeTypeStrategy.postEntityCreation(subThing);
			tx.success();
			return thing;
		} finally {
			tx.finish();
		}
	}

	@NodeEntity
	public static class Thing {

		String name;

		public Thing() {
		}
		public Thing(Node n) {
			setPersistentState(n);
		}

	}

	public static class SubThing extends Thing {

		public SubThing() {
			super();
		}

		public SubThing(Node n) {
			super(n);
		}
	}

	private static Set<Node> set(Node... nodes) {
		return new HashSet<Node>(Arrays.asList(nodes));
	}

	private void manualCleanDb() {
		Transaction tx = graphDatabaseService.beginTx();
		try {
			cleanDb();
			tx.success();
		} finally {
			tx.finish();
		}
	}
}
