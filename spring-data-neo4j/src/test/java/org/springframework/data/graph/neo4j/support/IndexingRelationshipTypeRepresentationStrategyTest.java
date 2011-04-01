package org.springframework.data.graph.neo4j.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.*;
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
        "classpath:org/springframework/data/graph/neo4j/support/IndexingTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class IndexingRelationshipTypeRepresentationStrategyTest {

	@Autowired
	private GraphDatabaseService graphDatabaseService;
	@Autowired
	private IndexingRelationshipTypeRepresentationStrategy relationshipTypeRepresentationStrategy;

    private Link link;

    @BeforeTransaction
	public void cleanDb() {
		Neo4jHelper.cleanDb(graphDatabaseService);
	}

	@Before
	public void setUp() throws Exception {
		if (link == null) {
			createThingsAndLinks();
		}
	}

	@Test
	@Transactional
	public void testPostEntityCreationOfRelationshipBacked() throws Exception {
		Index<Relationship> typesIndex = graphDatabaseService.index().forRelationships(IndexingNodeTypeRepresentationStrategy.INDEX_NAME);
		IndexHits<Relationship> linkHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, link.getClass().getName());
        Relationship rel = linkHits.getSingle();
        assertEquals(rel(link), rel);
		assertEquals(link.getClass().getName(), rel.getProperty("__type__"));
	}

	@Test
	public void testPreEntityRemovalOfRelationshipBacked() throws Exception {
        manualCleanDb();
        createThingsAndLinks();
		Index<Relationship> typesIndex = graphDatabaseService.index().forRelationships(IndexingNodeTypeRepresentationStrategy.INDEX_NAME);

        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            relationshipTypeRepresentationStrategy.preEntityRemoval(rel(link));
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        IndexHits<Relationship> linkHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, link.getClass().getName());
        assertNull(linkHits.getSingle());
	}

	@Test
	@Transactional
	public void testFindAllOfRelationshipBacked() throws Exception {
		assertEquals("Did not find all links.",
				Arrays.asList(link),
				IteratorUtil.addToCollection(relationshipTypeRepresentationStrategy.findAll(Link.class), new ArrayList<Link>()));
	}

	@Test
	@Transactional
	public void testCountOfRelationshipBacked() throws Exception {
		assertEquals(1, relationshipTypeRepresentationStrategy.count(Link.class));
	}

    @Test
    @Transactional
    public void testGetJavaTypeOfRelationshipBacked() throws Exception {
        assertEquals(Link.class, relationshipTypeRepresentationStrategy.getJavaType(rel(link)));
    }

	@Test
	@Transactional
	public void testCreateEntityAndInferType() throws Exception {
        Link newLink = relationshipTypeRepresentationStrategy.createEntity(rel(link));
        assertEquals(link, newLink);
    }

	@Test
	@Transactional
	public void testCreateEntityAndSpecifyType() throws Exception {
        Link newLink = relationshipTypeRepresentationStrategy.createEntity(rel(link), Link.class);
        assertEquals(link, newLink);
    }

    @Test
    @Transactional
	public void testProjectEntity() throws Exception {
        UnrelatedLink other = relationshipTypeRepresentationStrategy.projectEntity(rel(link), UnrelatedLink.class);
        assertEquals("link", other.getLabel());
	}

    private static Relationship rel(Link link) {
        return link.getPersistentState();
    }

	private void createThingsAndLinks() {
		Transaction tx = graphDatabaseService.beginTx();
		try {
			Node n1 = graphDatabaseService.createNode();
	        Node n2 = graphDatabaseService.createNode();
            Relationship rel = n1.createRelationshipTo(n2, DynamicRelationshipType.withName("link"));
            link = new Link(rel);
            relationshipTypeRepresentationStrategy.postEntityCreation(rel, Link.class);
            link.setLabel("link");
			tx.success();
		} finally {
			tx.finish();
		}
	}

    @RelationshipEntity
    public static class UnrelatedLink {
        String label;

        public String getLabel() {
            return label;
        }
    }

    @RelationshipEntity
    public static class Link {
        String label;

        public Link() {
        }

        public Link(Relationship rel) {
            setPersistentState(rel);
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class SubLink extends Link {
        public SubLink() {
        }

        public SubLink(Relationship rel) {
            super(rel);
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
