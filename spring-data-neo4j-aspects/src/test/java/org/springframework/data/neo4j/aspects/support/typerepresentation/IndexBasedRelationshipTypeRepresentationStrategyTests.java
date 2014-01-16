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
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.aspects.support.EntityTestBase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.support.typerepresentation.IndexBasedNodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.typerepresentation.IndexBasedRelationshipTypeRepresentationStrategy;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.helpers.collection.IteratorUtil.first;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/IndexingTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class IndexBasedRelationshipTypeRepresentationStrategyTests extends EntityTestBase {

	@Autowired
	private IndexBasedRelationshipTypeRepresentationStrategy relationshipTypeRepresentationStrategy;

    @Autowired EntityStateHandler entityStateHandler;
    @Autowired Neo4jTemplate neo4jTemplate;

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
		Index<Relationship> typesIndex = graphDatabaseService.index().forRelationships(IndexBasedRelationshipTypeRepresentationStrategy.INDEX_NAME);
        final Object alias = neo4jTemplate.getEntityType(Link.class).getAlias();
        IndexHits<Relationship> linkHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, alias);
        Relationship rel = linkHits.getSingle();
        assertEquals(rel(link), rel);
		assertEquals(alias, rel.getProperty("__type__"));
		linkHits.close();
	}

	@Test
	public void testPreEntityRemovalOfRelationshipBacked() throws Exception {
        manualCleanDb();
        createThingsAndLinks();

        try (Transaction tx = graphDatabaseService.beginTx()) {
            relationshipTypeRepresentationStrategy.preEntityRemoval(rel(link));
            tx.success();
        }

        try (Transaction tx = graphDatabaseService.beginTx()) {
            Index<Relationship> typesIndex = graphDatabaseService.index().forRelationships(IndexBasedNodeTypeRepresentationStrategy.INDEX_NAME);
            IndexHits<Relationship> linkHits = typesIndex.get(IndexBasedNodeTypeRepresentationStrategy.INDEX_KEY, link.getClass().getName());
            assertNull(linkHits.getSingle());
            tx.success();
        }
	}

	@Test
	@Transactional
	public void testFindAllOfRelationshipBacked() throws Exception {
        final Collection<Relationship> links = asCollection(relationshipTypeRepresentationStrategy.findAll(typeOf(Link.class)));
        assertEquals(1,links.size());
        assertEquals("Did not find all links.", neo4jTemplate.getPersistentState(link), first(links));
	}

	@Test
	@Transactional
	public void testCountOfRelationshipBacked() throws Exception {
		assertEquals(1, relationshipTypeRepresentationStrategy.count(typeOf(Link.class)));
	}

    @Test
    @Transactional
    public void testGetJavaTypeOfRelationshipBacked() throws Exception {
        assertEquals(typeOf(Link.class).getAlias(), relationshipTypeRepresentationStrategy.readAliasFrom(rel(link)));
        assertEquals(Link.class, neo4jTemplate.getStoredJavaType(link));
    }

	@Test
	@Transactional
	public void testCreateEntityAndInferType() throws Exception {
        Link newLink = neo4jTemplate.createEntityFromStoredType(rel(link), neo4jTemplate.getMappingPolicy(link));
        assertEquals(link, newLink);
    }

	@Test
	@Transactional
	public void testCreateEntityAndSpecifyType() throws Exception {
        Link newLink = neo4jTemplate.createEntityFromState(rel(link), Link.class, neo4jTemplate.getMappingPolicy(link));
        assertEquals(link, newLink);
    }

    @Test
    @Transactional
	public void testProjectEntity() throws Exception {
        UnrelatedLink other = neo4jTemplate.projectTo(rel(link), UnrelatedLink.class);
        assertEquals("link", other.getLabel());
	}

    private Relationship rel(Link link) {
        return getRelationshipState(link);
    }

	private void createThingsAndLinks() {
		Transaction tx = graphDatabaseService.beginTx();
		try {
			Node n1 = graphDatabaseService.createNode();
	        Node n2 = graphDatabaseService.createNode();
            Relationship rel = n1.createRelationshipTo(n2, DynamicRelationshipType.withName("link"));
            link = new Link();
            neo4jTemplate.setPersistentState(link,rel);
            relationshipTypeRepresentationStrategy.writeTypeTo(rel, neo4jTemplate.getEntityType(Link.class));
            link.setLabel("link");
			tx.success();
		} finally {
			tx.close();
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

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class SubLink extends Link {
    }

}
