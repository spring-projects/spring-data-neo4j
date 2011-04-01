package org.springframework.data.graph.neo4j.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml",
        "classpath:org/springframework/data/graph/neo4j/support/NoopTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class NoopTypeRepresentationStrategyTest {

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;
	@Autowired
	private NoopNodeTypeRepresentationStrategy noopNodeStrategy;
    @Autowired
	private NoopRelationshipTypeRepresentationStrategy noopRelationshipStrategy;

	private Thing thing;
    private Link link;

    @Before
	public void setUp() throws Exception {
		createThing();
	}

	@Test
	public void testPostEntityCreation() throws Exception {
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testFindAllForNodeStrategy() throws Exception {
		noopNodeStrategy.findAll(Thing.class);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testFindAllForRelationshipStrategy() throws Exception {
		noopRelationshipStrategy.findAll(Link.class);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testCountForNodeStrategy() throws Exception {
		noopNodeStrategy.count(Thing.class);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testCountForRelationshipStrategy() throws Exception {
		noopRelationshipStrategy.count(Link.class);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testGetJavaTypeOnNodeStrategy() throws Exception {
		noopNodeStrategy.getJavaType(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testGetJavaTypeOnRelationshipStrategy() throws Exception {
		noopRelationshipStrategy.getJavaType(null);
	}

	@Test
	public void testPreEntityRemoval() throws Exception {
        noopNodeStrategy.preEntityRemoval(node(thing));
        noopRelationshipStrategy.preEntityRemoval(rel(link));
    }

	private static Node node(Thing thing) {
		return thing.getPersistentState();
	}

	private static Relationship rel(Link link) {
		return link.getPersistentState();
	}

	private Thing createThing() {
		Transaction tx = graphDatabaseContext.beginTx();
		try {
			Node node = graphDatabaseContext.createNode();
			thing = new Thing(node);
			noopNodeStrategy.postEntityCreation(node, Thing.class);
            Relationship rel = node.createRelationshipTo(graphDatabaseContext.createNode(), DynamicRelationshipType.withName("link"));
            link = new Link(rel);
            noopRelationshipStrategy.postEntityCreation(rel, Link.class);
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

    @RelationshipEntity
    public static class Link {

        public Link() {
        }

        public Link(Relationship rel) {
            setPersistentState(rel);
        }
    }
}
