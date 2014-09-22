/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb.transaction;

import javax.transaction.*;
import javax.transaction.xa.XAResource;

public class NullTransactionManager implements TransactionManager {
    private static final Transaction TRANSACTION = new Transaction() {
        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SecurityException, SystemException {

        }

        @Override
        public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException, SystemException {
            return false;
        }

        @Override
        public boolean enlistResource(XAResource xaResource) throws IllegalStateException, RollbackException, SystemException {
            return false;
        }

        @Override
        public int getStatus() throws SystemException {
            return Status.STATUS_NO_TRANSACTION;
        }

        @Override
        public void registerSynchronization(Synchronization synchronization) throws IllegalStateException, RollbackException, SystemException {

        }

        @Override
        public void rollback() throws IllegalStateException, SystemException {

        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {

        }
    };

    @Override
    public void begin() throws NotSupportedException, SystemException {

    }

    @Override
    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {

    }

    @Override
    public int getStatus() throws SystemException {
        return 0;
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        return TRANSACTION;
    }

    @Override
    public void resume(Transaction transaction) throws IllegalStateException, InvalidTransactionException, SystemException {

    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {

    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {

    }

    @Override
    public void setTransactionTimeout(int i) throws SystemException {

    }

    @Override
    public Transaction suspend() throws SystemException {
        return TRANSACTION;
    }
}