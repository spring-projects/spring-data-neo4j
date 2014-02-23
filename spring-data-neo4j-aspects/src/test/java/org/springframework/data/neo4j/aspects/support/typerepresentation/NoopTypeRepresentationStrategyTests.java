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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.aspects.support.EntityTestBase;
import org.springframework.data.neo4j.support.typerepresentation.NoopNodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.typerepresentation.NoopRelationshipTypeRepresentationStrategy;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/NoopTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class NoopTypeRepresentationStrategyTests extends EntityTestBase {

	@Autowired
	private NoopNodeTypeRepresentationStrategy noopNodeStrategy;
    @Autowired
	private NoopRelationshipTypeRepresentationStrategy noopRelationshipStrategy;

	private NoopThing thing;
    private NoopLink link;

    @Before
	public void setUp() throws Exception {
		createThing();
	}

	@Test
	public void testPostEntityCreation() throws Exception {
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testFindAllForNodeStrategy() throws Exception {
		noopNodeStrategy.findAll(typeOf(NoopThing.class));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testFindAllForRelationshipStrategy() throws Exception {
		noopRelationshipStrategy.findAll(typeOf(NoopLink.class));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testCountForNodeStrategy() throws Exception {
		noopNodeStrategy.count(typeOf(NoopThing.class));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testCountForRelationshipStrategy() throws Exception {
		noopRelationshipStrategy.count(typeOf(NoopLink.class));
	}

	@Test
	public void testGetJavaTypeOnNodeStrategy() throws Exception {
        assertNull(noopNodeStrategy.readAliasFrom(null));
	}

	@Test
	public void testGetJavaTypeOnRelationshipStrategy() throws Exception {
		assertNull(noopRelationshipStrategy.readAliasFrom(null));
	}

	@Test
	public void testPreEntityRemoval() throws Exception {
        noopNodeStrategy.preEntityRemoval(node(thing));
        noopRelationshipStrategy.preEntityRemoval(rel(link));
    }

	private Node node(NoopThing thing) {
        return getNodeState(thing);
	}

	private Relationship rel(NoopLink link) {
        return getRelationshipState(link);
	}

	private NoopThing createThing() {
        Transaction tx = neo4jTemplate.getGraphDatabase().beginTx();
		try {
			Node node = neo4jTemplate.createNode();
			thing = new NoopThing();
            neo4jTemplate.setPersistentState(thing,node);
            noopNodeStrategy.writeTypeTo(node, typeOf(NoopThing.class));
            Relationship rel = node.createRelationshipTo(neo4jTemplate.createNode(), DynamicRelationshipType.withName("link"));
            link = new NoopLink();
            neo4jTemplate.setPersistentState(link,rel);
            noopRelationshipStrategy.writeTypeTo(rel, typeOf(NoopLink.class));
			tx.success();
			return thing;
		} finally {
			tx.close();
		}
	}

    @NodeEntity
	public static class NoopThing {
		String name;
	}

    @RelationshipEntity
    public static class NoopLink {
    }
}
