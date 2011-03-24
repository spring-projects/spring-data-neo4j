package org.springframework.data.graph.neo4j.config;

import org.neo4j.graphdb.Transaction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
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
