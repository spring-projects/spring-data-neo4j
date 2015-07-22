/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.ogm.metadata.MappingException;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.*;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.UnmanagedUserPojo;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class QueryIntegrationTest {

    @ClassRule
    public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule(7879);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @After
    public void clearDatabase() {
        neo4jRule.clearDatabase();
    }

    private void executeUpdate(String cypher) {
        new ExecutionEngine(neo4jRule.getGraphDatabaseService()).execute(cypher);
    }

    @Test
    public void shouldFindArbitraryGraph() {
        executeUpdate(
                "CREATE " +
                        "(dh:Movie {name:'Die Hard'}), " +
                        "(fe:Movie {name: 'The Fifth Element'}), " +
                        "(bw:User {name: 'Bruce Willis'}), " +
                        "(ar:User {name: 'Alan Rickman'}), " +
                        "(mj:User {name: 'Milla Jovovich'}), " +
                        "(mj)-[:ACTED_IN]->(fe), " +
                        "(ar)-[:ACTED_IN]->(dh), " +
                        "(bw)-[:ACTED_IN]->(dh), " +
                        "(bw)-[:ACTED_IN]->(fe)");

        List<Map<String, Object>> graph = userRepository.getGraph();
        assertNotNull(graph);
        int i = 0;
        for (Map<String,Object> properties: graph) {
            i++;
            assertNotNull(properties);
        }
        assertEquals(2, i);
    }

    @Test
    public void shouldFindScalarValues() {
        executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");
        List<Integer> ids = userRepository.getUserIds();
        assertEquals(2, ids.size());

        List<Long> nodeIds = userRepository.getUserNodeIds();
        assertEquals(2, nodeIds.size());
    }

    @Test
    public void shouldFindUserByName() {
        executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        User user = userRepository.findUserByName("Michal");
        assertEquals("Michal",user.getName());
    }

    @Test
    public void shouldFindTotalUsers() {
        executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        int users = userRepository.findTotalUsers();
        assertEquals(users, 2);
    }

    @Test
    public void shouldFindUsers() {
        executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        Collection<User> users = userRepository.getAllUsers();
        assertEquals(users.size(), 2);
    }

    @Test
    public void shouldFindUserByNameWithNamedParam() {
        executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        User user = userRepository.findUserByNameWithNamedParam("Michal");
        assertEquals("Michal",user.getName());
    }

    @Test
    public void shouldFindUsersAsProperties() {
        executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        Iterable<Map<String, Object>> users = userRepository.getUsersAsProperties();
        assertNotNull(users);
        int i = 0;
        for (Map<String,Object> properties: users) {
            i++;
            assertNotNull(properties);
        }
        assertEquals(2, i);
    }

    /**
     * @see DATAGRAPH-698
     */
    @Test
    public void shouldFindUsersAndMapThemToConcreteQueryResultObjectCollection() {
        executeUpdate("CREATE (g:User {name:'Gary', age:32}), (s:User {name:'Sheila', age:29}), (v:User {name:'Vince', age:66})");
        assertEquals("There should be some users in the database", 3, userRepository.findTotalUsers());

        Iterable<UserQueryResult> expected = Arrays.asList(new UserQueryResult("Sheila", 29),
                new UserQueryResult("Gary", 32), new UserQueryResult("Vince", 66));

        Iterable<UserQueryResult> queryResult = userRepository.retrieveAllUsersAndTheirAges();
        assertNotNull("The query result shouldn't be null", queryResult);
        assertEquals(expected, queryResult);
        for(UserQueryResult userQueryResult : queryResult) {
            assertNotNull(userQueryResult.getUserId());
        }
    }

    /**
     * This limitation about not handling unmanaged types may be addressed after M2 if there's demand for it.
     */
    @Test(expected = MappingException.class)
    public void shouldThrowMappingExceptionIfQueryResultTypeIsNotManagedInMappingMetadata() {
        executeUpdate("CREATE (:User {name:'Colin'}), (:User {name:'Jeff'})");

        // NB: UnmanagedUserPojo is not scanned with the other domain classes
        UnmanagedUserPojo queryResult = userRepository.findIndividualUserAsDifferentObject("Jeff");
        assertNotNull("The query result shouldn't be null", queryResult);
        assertEquals("Jeff", queryResult.getName());
    }

    @Test
    public void shouldFindUsersAndMapThemToProxiedQueryResultInterface() {
        executeUpdate("CREATE (:User {name:'Morne', age:30}), (:User {name:'Abraham', age:31}), (:User {name:'Virat', age:27})");

        UserQueryResultInterface result = userRepository.findIndividualUserAsProxiedObject("Abraham");
        assertNotNull("The query result shouldn't be null", result);
        assertEquals("The wrong user was returned", "Abraham", result.getNameOfUser());
        assertEquals("The wrong user was returned", 31, result.getAgeOfUser());
    }

    @Test
    public void shouldRetrieveUsersByGenderAndConvertToCorrectTypes() {
        executeUpdate("CREATE (:User {name:'David Warner', gender:'MALE'}), (:User {name:'Shikhar Dhawan', gender:'MALE'}), "
                + "(:User {name:'Sarah Taylor', gender:'FEMALE', account: '3456789', deposits:['12345.6','45678.9']})");

        Iterable<RichUserQueryResult> usersByGender = userRepository.findUsersByGender(Gender.FEMALE);
        assertNotNull("The resultant users list shouldn't be null", usersByGender);

        Iterator<RichUserQueryResult> userIterator = usersByGender.iterator();
        assertTrue(userIterator.hasNext());
        RichUserQueryResult userQueryResult = userIterator.next();
        assertEquals(Gender.FEMALE, userQueryResult.getUserGender());
        assertEquals("Sarah Taylor", userQueryResult.getUserName());
        assertEquals(BigInteger.valueOf(3456789), userQueryResult.getUserAccount());
        assertArrayEquals(new BigDecimal[]{BigDecimal.valueOf(12345.6), BigDecimal.valueOf(45678.9)}, userQueryResult.getUserDeposits());
        assertFalse(userIterator.hasNext());
    }

    /**
     * I'm not sure whether we should actually support this because you could just return an entity!
     */
    @Ignore
    @Test
    public void shouldMapNodeEntitiesIntoQueryResultObjects() {
        executeUpdate("CREATE (:User {name:'Abraham'}), (:User {name:'Barry'}), (:User {name:'Colin'})");

        EntityWrappingQueryResult wrappedUser = userRepository.findWrappedUserByName("Barry");
        assertNotNull("The loaded wrapper object shouldn't be null", wrappedUser);
        assertNotNull("The enclosed user shouldn't be null", wrappedUser.getUser());
        assertEquals("Barry", wrappedUser.getUser().getName());
    }


    /**
     * @see DATAGRAPH-694
     */
    @Test
    public void shouldSubstituteUserId() {
        executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        User michal = userRepository.findUserByName("Michal");
        assertNotNull(michal);
        User user = userRepository.loadUserById(michal);
        assertEquals("Michal",user.getName());
    }

    /**
     * @see DATAGRAPH-694
     */
    @Test
    public void shouldSubstituteNamedParamUserId() {
        executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        User michal = userRepository.findUserByName("Michal");
        assertNotNull(michal);
        User user = userRepository.loadUserByNamedId(michal);
        assertEquals("Michal",user.getName());
    }

}
