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

import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.tooling.GlobalGraphOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

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
                // the exception here does not get propagated to the caller if we're.
            }
        });
    }


    @Test(expected = Exception.class)
    public void whenImplicitTransactionFailsNothingShouldBeCreated() {
        try {
            userRepository.save(new User("Michal"));
            fail("should have thrown exception");
        } catch (Exception e) {
            parseExceptionMessage(e.getLocalizedMessage());
            checkDatabase();
        }

    }

    private void parseExceptionMessage(String localizedMessage) {
        String parsed = localizedMessage.replace("{", "{\n");
        parsed = parsed.replace("\\n\\tat", "\n\tat");
        parsed = parsed.replace("},{", "},\n{");
        parsed = parsed.replace("\\n", "\n");

        System.out.println(parsed);

    }

    @Test(expected = Exception.class)
    public void whenExplicitTransactionFailsNothingShouldBeCreated() {
        try {
            userService.saveWithTxAnnotationOnInterface(new User("Michal"));
            fail("should have thrown exception");
        } catch (Exception e) {
            parseExceptionMessage(e.getLocalizedMessage());
            checkDatabase();
        }

    }

    @Test(expected = Exception.class)
    public void whenExplicitTransactionFailsNothingShouldBeCreated2() {
        try {
            userService.saveWithTxAnnotationOnImpl(new User("Michal"));
            fail("should have thrown exception");
        } catch (Exception e) {
            parseExceptionMessage(e.getLocalizedMessage());
            checkDatabase();
        }
    }

    private void checkDatabase() {
        try (Transaction tx = neo4jRule.getGraphDatabaseService().beginTx()) {
            assertFalse(GlobalGraphOperations.at(neo4jRule.getGraphDatabaseService()).getAllNodes().iterator().hasNext());
            tx.success();
        }
    }

    static class TransactionInterceptException extends Exception {
        public TransactionInterceptException(String msg) {
            super(msg);
        }
    }

}
