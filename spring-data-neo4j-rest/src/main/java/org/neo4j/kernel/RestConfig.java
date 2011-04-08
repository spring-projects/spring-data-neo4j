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

package org.neo4j.kernel;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.core.*;
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction;
import org.neo4j.kernel.impl.nioneo.store.StoreId;
import org.neo4j.kernel.impl.transaction.LockManager;
import org.neo4j.kernel.impl.transaction.TxModule;
import org.neo4j.kernel.impl.transaction.xaframework.LogBufferFactory;
import org.neo4j.kernel.impl.transaction.xaframework.TxIdGenerator;

import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.util.Collections;
import java.util.Map;

/**
* @author mh
* @since 23.02.11
*/
public class RestConfig extends Config {
    public RestConfig(GraphDatabaseService graphDb, String storeDir, StoreId storeId,
                      Map<String, String> inputParams, KernelPanicEventGenerator kpe,
                      TxModule txModule, LockManager lockManager, LockReleaser lockReleaser,
                      IdGeneratorFactory idGeneratorFactory,
                      TxEventSyncHookFactory txSyncHookFactory, RelationshipTypeCreator relTypeCreator,
                      TxIdGenerator txIdGenerator, LastCommittedTxIdSetter lastCommittedTxIdSetter,
                      FileSystemAbstraction fileSystem, LogBufferFactory logBufferFactory) {
        super(graphDb, storeDir, storeId,
                inputParams, kpe, txModule, lockManager, lockReleaser, idGeneratorFactory, txSyncHookFactory, relTypeCreator, txIdGenerator, lastCommittedTxIdSetter, fileSystem, logBufferFactory);
    }

    public RestConfig(AbstractGraphDatabase restGraphDatabase) {
        super(restGraphDatabase, restGraphDatabase.getStoreDir(), null,
                Collections.<String,String>emptyMap(),null,
                new TxModule(true,null){
                    @Override
                    public TransactionManager getTxManager() {
                        return new NullTransactionManager();
                    }
                },
                null,null,
                null,null,null,null,null,
                null,null);
    }

    private static class NullTransactionManager implements TransactionManager {
        private static final Transaction NULL_JAVA_TRANSACTION = new Transaction() {
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
            return NULL_JAVA_TRANSACTION;
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
            return NULL_JAVA_TRANSACTION;
        }
    }
}