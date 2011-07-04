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

package org.springframework.data.graph.neo4j.support.query;

import org.junit.Before;
import org.junit.Test;
import org.junit.internal.matchers.IsCollectionContaining;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.GraphDatabase;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.Personality;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.TestTeam;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


/**
 * @author mh
 * @since 13.06.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
@Transactional
public class QueryOperationsTest {
    @Autowired
    protected ConversionService conversionService;
    @Autowired
    private GraphDatabaseContext graphDatabaseContext;
    private QueryOperations queryOperations;
    private TestTeam testTeam;
    private Person michael;
    private GraphDatabase graphDatabase;

    @Before
    public void setUp() throws Exception {
        graphDatabase = createGraphDatabase();
        testTeam = new TestTeam();
        testTeam.createSDGTeam();
        queryOperations = new DefaultQueryOperations(graphDatabase.queryEngineFor(QueryEngine.Type.Cypher));
        michael = testTeam.michael;
    }

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    protected GraphDatabase createGraphDatabase() throws Exception {
        final DelegatingGraphDatabase graphDatabase = new DelegatingGraphDatabase(graphDatabaseContext.getGraphDatabaseService());
        graphDatabase.setConversionService(conversionService);
        return graphDatabase;
    }

    @Test
    @Transactional
    public void testQueryList() throws Exception {
        final String queryString = String.format("start person=(%d,%d) return person.name, person.age", idFor(michael), idFor(testTeam.david));
        final Collection<Map<String,Object>> result = IteratorUtil.asCollection(queryOperations.queryForList(queryString));

        assertEquals(asList(testTeam.simpleRowFor(michael,"person"),testTeam.simpleRowFor(testTeam.david,"person")),result);
    }

    @Test
    public void testQueryListOfTypePerson() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") match (person) <-[:boss]- (boss) return boss", michael.getName());
        final Collection<Node> result = IteratorUtil.asCollection(queryOperations.query(queryString, Node.class));

        assertEquals(asList(nodeFor(testTeam.emil)),result);

    }

    private Node nodeFor(final NodeBacked entity) {
        return entity.getPersistentState();
    }
    private long idFor(final NodeBacked entity) {
        return entity.getNodeId();
    }

    @Test
    public void testQueryOtherTeamMembers() throws Exception {
        final String queryString = String.format("start person=(%d) match (person)<-[:persons]-(team)-[:persons]->(member) return member", idFor(michael));
        System.out.println("testTeam = " + testTeam.sdg.getPersons());
        final Collection<Node> result = IteratorUtil.asCollection(queryOperations.query(queryString, Node.class));

        assertThat(result, IsCollectionContaining.hasItems(nodeFor(testTeam.david), nodeFor(testTeam.emil)));

    }

    @Test
    public void testQueryAllTeamMembersByTeam() throws Exception {
        final String queryString = String.format("start team=(Group,name,\"%s\") match (team)-[:persons]->(member) return member", testTeam.sdg.getName());
        final Collection<Node> result = IteratorUtil.asCollection(queryOperations.query(queryString, Node.class));

        assertThat(result, IsCollectionContaining.hasItems(nodeFor(testTeam.david),nodeFor(testTeam.michael)));
    }

    @Test
    public void testQueryForObjectAsGroup() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") match (person) <-[:persons]- (team) return team", michael.getName());
        final Node result = queryOperations.queryForObject(queryString, Node.class);

        assertEquals(nodeFor(testTeam.sdg),result);
    }
    @Test
    public void testQueryForObjectAsString() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") match (person) <-[:persons]- (team) return team.name", michael.getName());
        final String result = queryOperations.queryForObject(queryString, String.class);

        assertEquals(testTeam.sdg.getName(),result);
    }
    @Test
    public void testQueryForObjectAsEnum() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") return person.personality", michael.getName());
        final Personality result = queryOperations.queryForObject(queryString, Personality.class);

        assertEquals(michael.getPersonality(),result);
    }
}
