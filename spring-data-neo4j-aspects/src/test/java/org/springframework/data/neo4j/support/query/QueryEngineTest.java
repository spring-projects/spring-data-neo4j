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

package org.springframework.data.neo4j.support.query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.Person;
import org.springframework.data.neo4j.Personality;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.QueryResult;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.TestTeam;
import org.springframework.data.neo4j.support.conversion.EntityResultConverter;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;


/**
 * @author mh
 * @since 13.06.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
@Transactional
public class QueryEngineTest {
    @Autowired
    protected ConversionService conversionService;
    @Autowired
    private GraphDatabaseContext graphDatabaseContext;
    private QueryEngine<Map<String,Object>> queryEngine;
    private TestTeam testTeam;
    private Person michael;
    private GraphDatabase graphDatabase;

    @Before
    public void setUp() throws Exception {
        graphDatabase = createGraphDatabase();
        testTeam = new TestTeam();
        testTeam.createSDGTeam();
        queryEngine = graphDatabase.queryEngineFor(QueryType.Cypher);
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
        final String queryString = "start person=(%michael,%david) return person.name, person.age";
        final Collection<Map<String,Object>> result = IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("michael",idFor(michael), "david",idFor(testTeam.david))));

        assertEquals(asList(testTeam.simpleRowFor(michael,"person"),testTeam.simpleRowFor(testTeam.david,"person")),result);
    }

    @Test
    public void testQueryListOfTypeNode() throws Exception {
        final String queryString = "start person=(name_index,name,\"%name\") match (person) <-[:boss]- (boss) return boss";
        final QueryResult<Map<String,Object>> queryResult = queryEngine.query(queryString, michaelsName());
        final Collection<Node> result = IteratorUtil.asCollection(queryResult.to(Node.class));

        assertEquals(asList(nodeFor(testTeam.emil)),result);
    }
    @Test
    public void testQueryListOfTypePerson() throws Exception {
        final String queryString = "start person=(name_index,name,\"%name\") match (person) <-[:boss]- (boss) return boss";
        final Collection<Person> result = IteratorUtil.asCollection(queryEngine.query(queryString, michaelsName()).to(Person.class, new EntityResultConverter(graphDatabaseContext)));

        assertEquals(asList(testTeam.emil),result);
    }

    private Map<String, Object> michaelsName() {
        return MapUtil.map("name", michael.getName());
    }

    @Test
    public void testQuerySingleOfTypePerson() throws Exception {
        final String queryString = "start person=(name_index,name,\"%name\") match (person) <-[:boss]- (boss) return boss";
        final Person result = queryEngine.query(queryString, michaelsName()).to(Person.class, new EntityResultConverter<Map<String,Object>,Person>(graphDatabaseContext)).single();

        assertEquals(testTeam.emil,result);
    }

    @Test
    public void testQueryListWithCustomConverter() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") match (person) <-[:boss]- (boss) return boss", michael.getName());
        final Collection<String> result = IteratorUtil.asCollection(queryEngine.query(queryString, michaelsName()).to(String.class, new ResultConverter<Map<String, Object>, String>() {
            @Override
            public String convert(Map<String, Object> row, Class<String> target) {
                return (String) ((Node) row.get("boss")).getProperty("name");
            }
        }));

        assertEquals(asList("Emil"),result);
    }

    private Node nodeFor(final NodeBacked entity) {
        return entity.getPersistentState();
    }
    private long idFor(final NodeBacked entity) {
        return entity.getNodeId();
    }

    @Test
    public void testQueryForObjectAsString() throws Exception {
        final String queryString = "start person=(name_index,name,\"%name\") match (person) <-[:persons]- (team) return team.name";
        final String result = queryEngine.query(queryString, michaelsName()).to(String.class).single();

        assertEquals(testTeam.sdg.getName(),result);
    }
    @Test
    public void testQueryForObjectAsEnum() throws Exception {
        final String queryString = "start person=(name_index,name,\"%name\") return person.personality";
        final Personality result = queryEngine.query(queryString, michaelsName()).to(Personality.class).single();

        assertEquals(michael.getPersonality(),result);
    }
}
