package org.springframework.data.graph.neo4j.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
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
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class IndexingTypeRepresentationStrategyTest {

	@Autowired
	private GraphDatabaseService graphDatabaseService;
	@Autowired
	private IndexingTypeRepresentationStrategy typeRepresentationStrategy;

	private Thing thing;
	private SubThing subThing;
    private Link link;

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
	public void testPostEntityCreationOfNodeBacked() throws Exception {
		Index<Node> typesIndex = graphDatabaseService.index().forNodes(IndexingTypeRepresentationStrategy.INDEX_NAME);
		IndexHits<Node> thingHits = typesIndex.get(IndexingTypeRepresentationStrategy.INDEX_KEY, thing.getClass().getName());
		assertEquals(set(node(thing), node(subThing)), IteratorUtil.addToCollection((Iterable<Node>)thingHits, new HashSet<Node>()));
		IndexHits<Node> subThingHits = typesIndex.get(IndexingTypeRepresentationStrategy.INDEX_KEY, subThing.getClass().getName());
		assertEquals(node(subThing), subThingHits.getSingle());
		assertEquals(thing.getClass().getName(), node(thing).getProperty(IndexingTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
		assertEquals(subThing.getClass().getName(), node(subThing).getProperty(IndexingTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
	}

	@Test
	public void testPreEntityRemovalOfNodeBacked() throws Exception {
        manualCleanDb();
        createThingsAndLinks();
		Index<Node> typesIndex = graphDatabaseService.index().forNodes(IndexingTypeRepresentationStrategy.INDEX_NAME);
		IndexHits<Node> thingHits;
		IndexHits<Node> subThingHits;

        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            typeRepresentationStrategy.preEntityRemoval(thing);
            tx.success();
        }
        finally
        {
            tx.finish();
        }

		thingHits = typesIndex.get(IndexingTypeRepresentationStrategy.INDEX_KEY, thing.getClass().getName());
		assertEquals(node(subThing), thingHits.getSingle());
		subThingHits = typesIndex.get(IndexingTypeRepresentationStrategy.INDEX_KEY, subThing.getClass().getName());
		assertEquals(node(subThing), subThingHits.getSingle());

        tx = graphDatabaseService.beginTx();
        try
        {
            typeRepresentationStrategy.preEntityRemoval(subThing);
            tx.success();
        }
        finally
        {
            tx.finish();
        }

		thingHits = typesIndex.get(IndexingTypeRepresentationStrategy.INDEX_KEY, thing.getClass().getName());
        assertNull(thingHits.getSingle());
		subThingHits = typesIndex.get(IndexingTypeRepresentationStrategy.INDEX_KEY, subThing.getClass().getName());
        assertNull(subThingHits.getSingle());
	}

	@Test
	@Transactional
	public void testFindAllOfNodeBacked() throws Exception {
		assertEquals("Did not find all things.",
				new HashSet<Thing>(Arrays.asList(subThing, thing)),
				IteratorUtil.addToCollection(typeRepresentationStrategy.findAll(Thing.class), new HashSet<Thing>()));
	}

	@Test
	@Transactional
	public void testCountOfNodeBacked() throws Exception {
		assertEquals(2, typeRepresentationStrategy.count(Thing.class));
	}

	@Test
	@Transactional
	public void testGetJavaTypeOfNodeBacked() throws Exception {
		assertEquals(Thing.class, typeRepresentationStrategy.getJavaType(node(thing)));
		assertEquals(SubThing.class, typeRepresentationStrategy.getJavaType(node(subThing)));
	}

	@Test
	@Transactional
	public void testConfirmTypeOfNodeBacked() throws Exception {
		assertEquals(Thing.class, typeRepresentationStrategy.confirmType(node(thing), Thing.class));
		assertEquals(SubThing.class, typeRepresentationStrategy.confirmType(node(subThing), Thing.class));
	}

	@Test
	@Transactional
	public void testPostEntityCreationOfRelationshipBacked() throws Exception {
		Index<Relationship> typesIndex = graphDatabaseService.index().forRelationships(IndexingTypeRepresentationStrategy.INDEX_NAME);
		IndexHits<Relationship> linkHits = typesIndex.get(IndexingTypeRepresentationStrategy.INDEX_KEY, link.getClass().getName());
        Relationship rel = linkHits.getSingle();
        assertEquals(rel(link), rel);
		assertEquals(link.getClass().getName(), rel.getProperty("__type__"));
	}

	@Test
	public void testPreEntityRemovalOfRelationshipBacked() throws Exception {
        manualCleanDb();
        createThingsAndLinks();
		Index<Relationship> typesIndex = graphDatabaseService.index().forRelationships(IndexingTypeRepresentationStrategy.INDEX_NAME);

        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            typeRepresentationStrategy.preEntityRemoval(link);
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        IndexHits<Relationship> linkHits = typesIndex.get(IndexingTypeRepresentationStrategy.INDEX_KEY, link.getClass().getName());
        assertNull(linkHits.getSingle());
	}

	@Test
	@Transactional
	public void testFindAllOfRelationshipBacked() throws Exception {
		assertEquals("Did not find all links.",
				Arrays.asList(link),
				IteratorUtil.addToCollection(typeRepresentationStrategy.findAll(Link.class), new ArrayList<Link>()));
	}

	@Test
	@Transactional
	public void testCountOfRelationshipBacked() throws Exception {
		assertEquals(1, typeRepresentationStrategy.count(Link.class));
	}

    @Test
    @Transactional
    public void testGetJavaTypeOfRelationshipBacked() throws Exception {
        assertEquals(Link.class, typeRepresentationStrategy.getJavaType(rel(link)));
    }

	@Test
	@Transactional
	public void testConfirmTypeOfRelationshipBacked() throws Exception {
		assertEquals(Link.class, typeRepresentationStrategy.confirmType(rel(link), Link.class));
	}

	private static Node node(Thing thing) {
		return thing.getPersistentState();
	}

    private static Relationship rel(Link link) {
        return link.getPersistentState();
    }

	private Thing createThingsAndLinks() {
		Transaction tx = graphDatabaseService.beginTx();
		try {
			thing = new Thing(graphDatabaseService.createNode());
			typeRepresentationStrategy.postEntityCreation(thing);
	        subThing = new SubThing(graphDatabaseService.createNode());
			typeRepresentationStrategy.postEntityCreation(subThing);
            link = thing.linkTo(subThing);
            typeRepresentationStrategy.postEntityCreation(link);
			tx.success();
			return thing;
		} finally {
			tx.finish();
		}
	}

	@NodeEntity
	public static class Thing {
		String name;
        Link link;

        public Thing(Node node) {
            setPersistentState(node);
        }

        public Link linkTo(Thing thing) {
            return relateTo(thing, Link.class, "link");
        }
    }

	public static class SubThing extends Thing {
        public SubThing(Node node) {
            super(node);
        }
    }

    @RelationshipEntity
    public static class Link {
        String label;
        @StartNode
        Thing start;
        @EndNode
        Thing end;

        public Link() {
        }

        public Link(String label) {
            this.label = label;
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
