/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc.", "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.movies;

import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.movies.context.PersistenceContext;
import org.springframework.data.neo4j.integration.movies.domain.User;
import org.springframework.data.neo4j.integration.movies.repo.UserRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Vince Bickers
 */
@ContextConfiguration(classes = {PersistenceContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class QueryIntegrationTest extends WrappingServerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected int neoServerPort() {
        return 7879;
    }

    @Test
    public void shouldFindArbitraryGraph() {
        new ExecutionEngine(getDatabase()).execute(
                "CREATE " +
                        "(dh:Movie {title:'Die Hard'}), " +
                        "(fe:Movie {title: 'The Fifth Element'}), " +
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
    public void shouldFindUsersByName() {
        new ExecutionEngine(getDatabase()).execute("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        Collection<User> users = userRepository.findByName("Michal");
        Iterator<User> iterator = users.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("Michal", iterator.next().getName());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldFindUsersByMiddleName() {
        new ExecutionEngine(getDatabase()).execute("CREATE (m:User {middleName:'Joseph'})<-[:FRIEND_OF]-(a:User {middleName:'Mary'})");

        Collection<User> users = userRepository.findByMiddleName("Joseph");
        Iterator<User> iterator = users.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("Joseph", iterator.next().getMiddleName());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldFindScalarValues() {
        new ExecutionEngine(getDatabase()).execute("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");
        List<Integer> ids = userRepository.getUserIds();
        assertEquals(2, ids.size());
    }

    @Test
    public void shouldFindUserByName() {
        new ExecutionEngine(getDatabase()).execute("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        User user = userRepository.findUserByName("Michal");
        assertEquals("Michal",user.getName());
    }

    @Test
    public void shouldFindTotalUsers() {
        new ExecutionEngine(getDatabase()).execute("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        int users = userRepository.findTotalUsers();
        assertEquals(users, 2);
    }

    @Test
    public void shouldFindUsers() {
        new ExecutionEngine(getDatabase()).execute("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        Collection<User> users = userRepository.getAllUsers();
        assertEquals(users.size(), 2);
    }

    @Test
    public void shouldFindUserByNameWithNamedParam() {
        new ExecutionEngine(getDatabase()).execute("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        User user = userRepository.findUserByNameWithNamedParam("Michal");
        assertEquals("Michal",user.getName());
    }

    @Test
    public void shouldFindUsersAsProperties() {
        new ExecutionEngine(getDatabase()).execute("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

        Iterable<Map<String, Object>> users = userRepository.getUsersAsProperties();
        assertNotNull(users);
        int i = 0;
        for (Map<String,Object> properties: users) {
            i++;
            assertNotNull(properties);
        }
        assertEquals(2, i);
    }

}
