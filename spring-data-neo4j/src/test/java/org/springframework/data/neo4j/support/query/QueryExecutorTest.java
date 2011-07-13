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
import org.junit.internal.matchers.IsCollectionContaining;
import org.junit.runner.RunWith;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.Group;
import org.springframework.data.neo4j.*;
import org.springframework.data.neo4j.Person;
import org.springframework.data.neo4j.Personality;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.TestTeam;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
@Transactional
public class QueryExecutorTest {
    @Autowired
    GraphDatabaseContext graphDatabaseContext;
    private QueryExecutor queryExecutor;
    private TestTeam testTeam;
    private Person michael;

    @Before
    public void setUp() throws Exception {
        testTeam = new TestTeam();
        testTeam.createSDGTeam();
        queryExecutor = new QueryExecutor(graphDatabaseContext);
        michael = testTeam.michael;
    }

    @Test
    @Transactional
    public void testQueryList() throws Exception {
        final String queryString = String.format("start person=(%d,%d) return person.name, person.age", michael.getNodeId(), testTeam.david.getNodeId());
        final Collection<Map<String,Object>> result = IteratorUtil.asCollection(queryExecutor.queryForList(queryString));

        assertEquals(asList(testTeam.simpleRowFor(michael,"person"),testTeam.simpleRowFor(testTeam.david,"person")),result);
    }

    @Test
    public void testQueryListOfTypePerson() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") match (person) <-[:boss]- (boss) return boss", michael.getName());
        final Collection<Person> result = IteratorUtil.asCollection(queryExecutor.query(queryString, Person.class));

        assertEquals(asList(testTeam.emil),result);

    }

    @Test
    public void testQueryOtherTeamMembers() throws Exception {
        final String queryString = String.format("start person=(%d) match (person)<-[:persons]-(team)-[:persons]->(member) return member", michael.getNodeId());
        System.out.println("testTeam = " + testTeam.sdg.getPersons());
        final Collection<Person> result = IteratorUtil.asCollection(queryExecutor.query(queryString, Person.class));

        assertThat(result, IsCollectionContaining.hasItems(testTeam.david, testTeam.emil));

    }

    @Test
    public void testQueryAllTeamMembersByTeam() throws Exception {
        final String queryString = String.format("start team=(Group,name,\"%s\") match (team)-[:persons]->(member) return member", testTeam.sdg.getName());
        final Collection<Person> result = IteratorUtil.asCollection(queryExecutor.query(queryString, Person.class));

        assertThat(result, IsCollectionContaining.hasItems(testTeam.david,testTeam.michael));
    }

    @Test
    public void testQueryForObjectAsGroup() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") match (person) <-[:persons]- (team) return team", michael.getName());
        final Group result = queryExecutor.queryForObject(queryString, Group.class);

        assertEquals(testTeam.sdg,result);
    }
    @Test
    public void testQueryForObjectAsString() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") match (person) <-[:persons]- (team) return team.name", michael.getName());
        final String result = queryExecutor.queryForObject(queryString, String.class);

        assertEquals(testTeam.sdg.getName(),result);
    }
    @Test
    public void testQueryForObjectAsEnum() throws Exception {
        final String queryString = String.format("start person=(name_index,name,\"%s\") return person.personality", michael.getName());
        final Personality result = queryExecutor.queryForObject(queryString, Personality.class);

        assertEquals(michael.getPersonality(),result);
    }
}
