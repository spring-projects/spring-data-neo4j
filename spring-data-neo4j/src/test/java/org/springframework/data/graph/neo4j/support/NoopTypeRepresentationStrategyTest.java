package org.springframework.data.graph.neo4j.support;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml",
		"classpath:org/springframework/data/graph/neo4j/support/NoopNodeTypeStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class NoopTypeRepresentationStrategyTest {
//
//	@Autowired
//	private GraphDatabaseContext graphDatabaseContext;
//	@Autowired
//	private NoopTypeRepresentationStrategy nodeTypeStrategy;
//
//	private Thing thing;
//
//	@Before
//	public void setUp() throws Exception {
//		thing = createThing();
//	}

	@Test
	public void testPostEntityCreation() throws Exception {
	}
//
//	@Test(expected = UnsupportedOperationException.class)
//	public void testFindAll() throws Exception {
//		nodeTypeStrategy.findAll(Thing.class);
//	}
//
//	@Test(expected = UnsupportedOperationException.class)
//	public void testCount() throws Exception {
//		nodeTypeStrategy.count(Thing.class);
//	}
//
//	@Test(expected = UnsupportedOperationException.class)
//	public void testGetJavaType() throws Exception {
//		nodeTypeStrategy.getJavaType(node(thing));
//	}
//
//	@Test
//	public void testPreEntityRemoval() throws Exception {
//		nodeTypeStrategy.preEntityRemoval(thing);
//	}
//
//	@Test
//	public void testConfirmType() throws Exception {
//		assertEquals(Thing.class, nodeTypeStrategy.confirmType(node(thing), Thing.class));
//	}
//
//	private static Node node(Thing thing) {
//		return thing.getPersistentState();
//	}
//
//	private Thing createThing() {
//		Transaction tx = graphDatabaseContext.beginTx();
//		try {
//			Node node = graphDatabaseContext.createNode();
//			Thing thing = new Thing(node);
//			nodeTypeStrategy.postEntityCreation(thing);
//			tx.success();
//			return thing;
//		} finally {
//			tx.finish();
//		}
//	}
//
//	@NodeEntity
//	public static class Thing {
//		String name;
//
//		public Thing() {
//		}
//
//		public Thing(Node n) {
//			setPersistentState(n);
//		}
//	}
}
