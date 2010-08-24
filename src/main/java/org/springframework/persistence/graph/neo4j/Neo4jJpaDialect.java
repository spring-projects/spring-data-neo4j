package org.springframework.persistence.graph.neo4j;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.orm.jpa.EntityManagerFactoryPlusOperations;
import org.springframework.orm.jpa.EntityManagerPlusOperations;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import java.sql.SQLException;

/**
 * @author Michael Hunger
 * @since 23.08.2010
 */
public class Neo4jJpaDialect implements JpaDialect {
    @Override
    public boolean supportsEntityManagerFactoryPlusOperations() {
        return false;
    }

    @Override
    public boolean supportsEntityManagerPlusOperations() {
        return false;
    }

    @Override
    public EntityManagerFactoryPlusOperations getEntityManagerFactoryPlusOperations(final EntityManagerFactory entityManagerFactory) {
        return null;
    }

    @Override
    public EntityManagerPlusOperations getEntityManagerPlusOperations(final EntityManager entityManager) {
        return null;
    }

    // todo
    @Override
    public Object beginTransaction(final EntityManager entityManager, final TransactionDefinition transactionDefinition) throws PersistenceException, SQLException, TransactionException {
        return null;
    }

    @Override
    public Object prepareTransaction(final EntityManager entityManager, final boolean b, final String s) throws PersistenceException {
        return null;
    }

    @Override
    public void cleanupTransaction(final Object o) {
    }

    @Override
    public ConnectionHandle getJdbcConnection(final EntityManager entityManager, final boolean b) throws PersistenceException, SQLException {
        return null;
    }

    @Override
    public void releaseJdbcConnection(final ConnectionHandle connectionHandle, final EntityManager entityManager) throws PersistenceException, SQLException {
    }

    // todo
    @Override
    public DataAccessException translateExceptionIfPossible(final RuntimeException e) {
        return null;
    }
}
