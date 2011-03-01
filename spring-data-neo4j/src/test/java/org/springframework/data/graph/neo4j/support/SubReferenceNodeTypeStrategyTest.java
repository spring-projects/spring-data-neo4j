package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;
import org.springframework.data.graph.neo4j.Car;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.Toyota;
import org.springframework.data.graph.neo4j.Volvo;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 20.01.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class SubReferenceNodeTypeStrategyTest {

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    GraphDatabaseContext graphDatabaseContext;
    @Autowired
    private FinderFactory finderFactory;

    private NodeTypeStrategy nodeTypeStrategy;
    private Node thingNode;
    private Thing thing;


    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Before
    public void setUp() {
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
        Transaction tx = graphDatabaseContext.beginTx();
        try {
            Node node = graphDatabaseContext.createNode();
            thing = new Thing(node);
            nodeTypeStrategy.postEntityCreation(thing);
            tx.success();
            return node;
        } finally {
            tx.finish();
        }

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
    public void testConfirmType() throws Exception {
        Assert.assertEquals("class in graph is thing", Thing.class, nodeTypeStrategy.confirmType(thingNode,Thing.class));

    }

    @Test
    @Transactional
    public void testFindAll() throws Exception {
        Collection<Thing> things = IteratorUtil.asCollection(nodeTypeStrategy.findAll(Thing.class));
        Assert.assertEquals("one thing created and found", 1, things.size());
        Assert.assertTrue("result only contains Thing", things.iterator().next() instanceof Thing);
    }


    @Test
	@Transactional
	public void testInstantiateConcreteClass() {
		log.debug("testInstantiateConcreteClass");
        Person p = new Person("Michael", 35).persist();
		Car c = new Volvo().persist();
		p.setCar(c);
		assertEquals("Wrong concrete class.", Volvo.class, p.getCar().getClass());
	}

	@Test
	@Transactional
	public void testInstantiateConcreteClassWithFinder() {
		log.debug("testInstantiateConcreteClassWithFinder");
		Volvo v=new Volvo().persist();
        NodeFinder<Car> finder = finderFactory.createNodeEntityFinder(Car.class);
		assertEquals("Wrong concrete class.", Volvo.class, finder.findAll().iterator().next().getClass());
	}

	@Test
	@Transactional
	public void testCountSubclasses() {
		log.warn("testCountSubclasses");
		new Volvo().persist();
		log.warn("Created volvo");
		new Toyota().persist();
		log.warn("Created volvo");
        assertEquals("Wrong count for Volvo.", 1, finderFactory.createNodeEntityFinder(Volvo.class).count());
        assertEquals("Wrong count for Toyota.", 1, finderFactory.createNodeEntityFinder(Toyota.class).count());
        assertEquals("Wrong count for Car.", 2, finderFactory.createNodeEntityFinder(Car.class).count());
	}
	@Test
	@Transactional
	public void testCountClasses() {
        new Person("Michael", 36).persist();
        new Person("David", 25).persist();
        assertEquals("Wrong Person instance count.", 2, finderFactory.createNodeEntityFinder(Person.class).count());
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
}
