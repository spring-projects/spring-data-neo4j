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

package org.springframework.data.neo4j.config;

import org.neo4j.graphdb.Transaction;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.PostConstruct;

/**
 * Validates correct configuration of Neo4j and Spring, especially transaction-managers
 */
public class ConfigurationCheck {
    GraphDatabaseContext graphDatabaseContext;
    PlatformTransactionManager transactionManager;

    public ConfigurationCheck(GraphDatabaseContext graphDatabaseContext, PlatformTransactionManager transactionManager) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.transactionManager = transactionManager;
    }

    @PostConstruct
    private void checkConfiguration() {
        checkInjection();
        checkSpringTransactionManager();
        checkNeo4jTransactionManager();
    }

    private void checkInjection() {
        assert graphDatabaseContext.getGraphDatabaseService()!=null : "graphDatabaseService not correctly configured, please refer to the manual, setup section";
    }

    private void checkSpringTransactionManager() {
        try {
            TransactionStatus transaction = transactionManager.getTransaction(null);
            updateStartTime();
            transactionManager.commit(transaction);
        } catch(Exception e) {
            AssertionError error = new AssertionError("transactionManager not correctly configured, please refer to the manual, setup section");
            error.initCause(e);
            throw error;
        }
    }

    private void checkNeo4jTransactionManager() {
        Transaction tx = null;
        try {
            tx = graphDatabaseContext.beginTx();
            updateStartTime();
            tx.success();
        } catch (Exception e) {
            AssertionError error = new AssertionError("transactionManager not correctly configured, please refer to the manual, setup section");
            error.initCause(e);
            throw error;
        } finally {
            try {
            if (tx != null) tx.finish();
            } catch(Exception e) {
                // ignore
            }
        }
    }

    private void updateStartTime() {
        graphDatabaseContext.getReferenceNode().setProperty("startTime", System.currentTimeMillis());
    }
}
