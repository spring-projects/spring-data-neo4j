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

import javax.persistence.EntityTransaction;
import javax.transaction.*;

/**
 * @author Michael Hunger
 * @since 20.08.2010
 */
class Neo4jEntityTransaction implements EntityTransaction {
    private final TransactionManager transactionManager;

    public Neo4jEntityTransaction(final TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void begin() {
        try {
            transactionManager.begin();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        } catch (NotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void commit() {
        try {
            transactionManager.commit();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        } catch (HeuristicRollbackException e) {
            throw new IllegalStateException(e);
        } catch (HeuristicMixedException e) {
            throw new IllegalStateException(e);
        } catch (RollbackException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            transactionManager.rollback();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setRollbackOnly() {
        try {
            transactionManager.setRollbackOnly();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean getRollbackOnly() {
        try {
            return transactionManager.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isActive() {
        try {
            return transactionManager.getStatus() == Status.STATUS_ACTIVE;
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }
}
