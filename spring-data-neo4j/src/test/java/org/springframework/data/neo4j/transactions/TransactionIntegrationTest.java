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

package org.springframework.data.neo4j.transactions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.ogm.session.result.ResultProcessingException;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Michal Bachman
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionIntegrationTest {

    @Rule
    public final Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule(7879);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Before
    public void populateDatabase() {
        neo4jRule.getGraphDatabaseService().registerTransactionEventHandler(new TransactionEventHandler.Adapter<Object>() {
            @Override
            public Object beforeCommit(TransactionData data) throws Exception {
                System.out.println("The request to commit is denied");
                throw new TransactionInterceptException("Deliberate testing exception");
            }
        });
    }

    @Test(expected = ResultProcessingException.class)
    public void whenImplicitTransactionFailsNothingShouldBeCreated() {
        userRepository.save(new User("Michal"));
    }

    @Test(expected = ResultProcessingException.class)
    public void whenExplicitTransactionFailsNothingShouldBeCreated() {
        userService.saveWithTxAnnotationOnInterface(new User("Michal"));
    }

    @Test(expected = ResultProcessingException.class)
    public void whenExplicitTransactionFailsNothingShouldBeCreated2() {
        userService.saveWithTxAnnotationOnImpl(new User("Michal"));
    }

    static class TransactionInterceptException extends Exception {
        public TransactionInterceptException(String msg) {
            super(msg);
        }
    }

}
