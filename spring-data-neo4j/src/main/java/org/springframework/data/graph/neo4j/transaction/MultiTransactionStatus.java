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

package org.springframework.data.graph.neo4j.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 14.02.11
 */
public class MultiTransactionStatus implements TransactionStatus {


    private PlatformTransactionManager mainTransactionManager;

    private Map<PlatformTransactionManager, TransactionStatus> transactionStatuses =
             Collections.synchronizedMap(new HashMap<PlatformTransactionManager, TransactionStatus>());

    private boolean newSynchonization;

    public MultiTransactionStatus(PlatformTransactionManager mainTransactionManager) {
        this.mainTransactionManager = mainTransactionManager;
    }


    private Map<PlatformTransactionManager, TransactionStatus> getTransactionStatuses() {
        return transactionStatuses;
    }

    private TransactionStatus getMainTransactionStatus() {
        return transactionStatuses.get(mainTransactionManager);
    }


    public void setNewSynchonization() {
        this.newSynchonization = true;
    }

    public boolean isNewSynchonization() {
        return newSynchonization;
    }


    @Override
    public boolean isNewTransaction() {
        return getMainTransactionStatus().isNewTransaction();
    }

    @Override
    public boolean hasSavepoint() {
        return getMainTransactionStatus().hasSavepoint();
    }

    @Override
    public void setRollbackOnly() {
        for(TransactionStatus ts : transactionStatuses.values() ){
            ts.setRollbackOnly();
        }
    }

    @Override
    public boolean isRollbackOnly() {
        return getMainTransactionStatus().isRollbackOnly();
    }

    @Override
    public boolean isCompleted() {
        return getMainTransactionStatus().isCompleted();
    }


    private static class SavePoints {
        Map<TransactionStatus,Object> savepoints=new HashMap<TransactionStatus, Object>();

        private void addSavePoint(TransactionStatus status, Object savepoint) {
            this.savepoints.put(status, savepoint);
        }

        private void save(TransactionStatus transactionStatus) {
            Object savepoint = transactionStatus.createSavepoint();
            addSavePoint(transactionStatus, savepoint);
        }


        public void rollback() {
            for (TransactionStatus transactionStatus : savepoints.keySet()) {
                transactionStatus.rollbackToSavepoint(savepointFor(transactionStatus));
            }
        }

        private Object savepointFor(TransactionStatus transactionStatus) {
            return savepoints.get(transactionStatus);
        }

        public void release() {
            for (TransactionStatus transactionStatus : savepoints.keySet()) {
                transactionStatus.releaseSavepoint(savepointFor(transactionStatus));
            }
        }
    }

    @Override
    public Object createSavepoint() throws TransactionException {
        SavePoints savePoints = new SavePoints();

        for (TransactionStatus transactionStatus : transactionStatuses.values()) {
            savePoints.save(transactionStatus);
        }
        return savePoints;
    }

    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        SavePoints savePoints= (SavePoints) savepoint;
        savePoints.rollback();
    }

    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        ((SavePoints)savepoint).release();
    }

    public void registerTransactionManager(TransactionDefinition definition, PlatformTransactionManager transactionManager) {
        getTransactionStatuses().put(transactionManager, transactionManager.getTransaction(definition));
    }

    void commit(PlatformTransactionManager transactionManager) {
        TransactionStatus transactionStatus = getTransactionStatus(transactionManager);
        transactionManager.commit(transactionStatus);
    }

    private TransactionStatus getTransactionStatus(PlatformTransactionManager transactionManager) {
        return this.getTransactionStatuses().get(transactionManager);
    }

    void rollback(PlatformTransactionManager transactionManager) {
        transactionManager.rollback(getTransactionStatus(transactionManager));
    }

    @Override
    public void flush() {
        for (TransactionStatus transactionStatus : transactionStatuses.values()) {
            transactionStatus.flush();
        }
    }
}