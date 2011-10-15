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

package org.springframework.data.neo4j.aspects.support.query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.support.EntityTestBase;
import org.springframework.data.neo4j.aspects.support.TestTeam;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;


/**
 * @author mh
 * @since 13.06.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml"})
@Transactional
public class GremlinQueryEngineTest extends EntityTestBase {
    @Autowired
    protected ConversionService conversionService;
    @Autowired
    private GraphDatabaseContext graphDatabaseContext;
    private QueryEngine<Object> queryEngine;
    private TestTeam testTeam;
    private Person michael;

    @Before
    public void setUp() throws Exception {
        GraphDatabase graphDatabase = createGraphDatabase();
        testTeam = new TestTeam();
        testTeam.createSDGTeam();
        queryEngine = graphDatabase.queryEngineFor(QueryType.Gremlin);
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

    @SuppressWarnings("unchecked")
    @Test
    @Transactional
    public void testQueryList() throws Exception {
        final String queryString = "t = new Table(); [g.v(michael),g.v(david)].each{ n -> n.as('person.name').as('person.age').table(t,['person.name','person.age']){ it.age }{ it.name } >> -1}; t;" ;
        final Collection<Object> result = IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("michael", getNodeId(michael), "david", getNodeId(testTeam.david))));

        assertEquals(asList(testTeam.simpleRowFor(michael, "person"), testTeam.simpleRowFor(testTeam.david, "person")), result);
    }

}
