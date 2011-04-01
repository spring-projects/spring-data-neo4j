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

package org.springframework.data.graph.neo4j.jpa;

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
