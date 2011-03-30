package org.springframework.data.graph.neo4j.support;

import org.junit.Before;
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
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml",
        "classpath:org/springframework/data/graph/neo4j/support/IndexingTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class IndexingNodeTypeRepresentationStrategyTest {

	@Autowired
	private GraphDatabaseService graphDatabaseService;
	@Autowired
	private IndexingNodeTypeRepresentationStrategy nodeTypeRepresentationStrategy;

	private Thing thing;
	private SubThing subThing;

    @BeforeTransaction
	public void cleanDb() {
		Neo4jHelper.cleanDb(graphDatabaseService);
	}

	@Before
	public void setUp() throws Exception {
		if (thing == null) {
			createThingsAndLinks();
		}
	}

	@Test
	@Transactional
	public void testPostEntityCreation() throws Exception {
		Index<Node> typesIndex = graphDatabaseService.index().forNodes(IndexingNodeTypeRepresentationStrategy.INDEX_NAME);
		IndexHits<Node> thingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, thing.getClass().getName());
		assertEquals(set(node(thing), node(subThing)), IteratorUtil.addToCollection((Iterable<Node>)thingHits, new HashSet<Node>()));
		IndexHits<Node> subThingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, subThing.getClass().getName());
		assertEquals(node(subThing), subThingHits.getSingle());
		assertEquals(thing.getClass().getName(), node(thing).getProperty(IndexingNodeTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
		assertEquals(subThing.getClass().getName(), node(subThing).getProperty(IndexingNodeTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
	}

	@Test
	public void testPreEntityRemoval() throws Exception {
        manualCleanDb();
        createThingsAndLinks();
		Index<Node> typesIndex = graphDatabaseService.index().forNodes(IndexingNodeTypeRepresentationStrategy.INDEX_NAME);
		IndexHits<Node> thingHits;
		IndexHits<Node> subThingHits;

        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            nodeTypeRepresentationStrategy.preEntityRemoval(node(thing));
            tx.success();
        }
        finally
        {
            tx.finish();
        }

		thingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, thing.getClass().getName());
		assertEquals(node(subThing), thingHits.getSingle());
		subThingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, subThing.getClass().getName());
		assertEquals(node(subThing), subThingHits.getSingle());

        tx = graphDatabaseService.beginTx();
        try {
            nodeTypeRepresentationStrategy.preEntityRemoval(node(subThing));
            tx.success();
        }
        finally
        {
            tx.finish();
        }

		thingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, thing.getClass().getName());
        assertNull(thingHits.getSingle());
		subThingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, subThing.getClass().getName());
        assertNull(subThingHits.getSingle());
	}

	@Test
	@Transactional
	public void testFindAll() throws Exception {
		assertEquals("Did not find all things.",
				new HashSet<Thing>(Arrays.asList(subThing, thing)),
				IteratorUtil.addToCollection(nodeTypeRepresentationStrategy.findAll(Thing.class), new HashSet<Thing>()));
	}

	@Test
	@Transactional
	public void testCount() throws Exception {
		assertEquals(2, nodeTypeRepresentationStrategy.count(Thing.class));
	}

	@Test
	@Transactional
	public void testGetJavaType() throws Exception {
		assertEquals(Thing.class, nodeTypeRepresentationStrategy.getJavaType(node(thing)));
		assertEquals(SubThing.class, nodeTypeRepresentationStrategy.getJavaType(node(subThing)));
	}

	@Test
	@Transactional
	public void testCreateEntityAndInferType() throws Exception {
        Thing newThing = nodeTypeRepresentationStrategy.createEntity(node(thing));
        assertEquals(thing, newThing);
    }

	@Test
	@Transactional
	public void testCreateEntityAndSpecifyType() throws Exception {
        Thing newThing = nodeTypeRepresentationStrategy.createEntity(node(subThing), Thing.class);
        assertEquals(subThing, newThing);
    }

    @Test
    @Transactional
	public void testProjectEntity() throws Exception {
        Unrelated other = nodeTypeRepresentationStrategy.projectEntity(node(thing), Unrelated.class);
        assertEquals("thing", other.getName());
	}

	private static Node node(Thing thing) {
		return thing.getPersistentState();
	}

	private Thing createThingsAndLinks() {
		Transaction tx = graphDatabaseService.beginTx();
		try {
            Node n1 = graphDatabaseService.createNode();
            thing = new Thing(n1);
			nodeTypeRepresentationStrategy.postEntityCreation(n1, Thing.class);
            thing.setName("thing");
            Node n2 = graphDatabaseService.createNode();
            subThing = new SubThing(n2);
			nodeTypeRepresentationStrategy.postEntityCreation(n2, SubThing.class);
            subThing.setName("subThing");
			tx.success();
			return thing;
		} finally {
			tx.finish();
		}
	}

    @NodeEntity
    public static class Unrelated {
        String name;

        public String getName() {
            return name;
        }
    }

	@NodeEntity
	public static class Thing {
		String name;

        public Thing(Node node) {
            setPersistentState(node);
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

	public static class SubThing extends Thing {
        public SubThing(Node node) {
            super(node);
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
