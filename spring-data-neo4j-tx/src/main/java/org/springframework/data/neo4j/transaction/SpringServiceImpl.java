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

package org.springframework.data.neo4j.transaction;

import org.neo4j.kernel.api.KernelAPI;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.core.TransactionState;
import org.neo4j.kernel.impl.transaction.AbstractTransactionManager;
import org.neo4j.kernel.impl.transaction.TransactionStateFactory;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.kernel.impl.transaction.xaframework.XaDataSource;
import org.objectweb.jotm.Current;
import org.objectweb.jotm.TransactionResourceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.*;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Chris Gioran
 */
@Configurable
class SpringServiceImpl extends AbstractTransactionManager
{
    private PlatformTransactionManager transactionManager;

    private TransactionManager delegate;

    private final Map<Transaction, TransactionState> states = new WeakHashMap<Transaction, TransactionState>();
//    private final Map<Transaction, KernelTransaction> kernelTransactions = new WeakHashMap<Transaction, KernelTransaction>();
    private final TransactionStateFactory stateFactory;
    private XaDataSourceManager xaDataSourceManager;
    private KernelAPI kernelAPI;

    SpringServiceImpl(TransactionStateFactory stateFactory, XaDataSourceManager xaDataSourceManager)
    {
        this.stateFactory = stateFactory;
        this.xaDataSourceManager = xaDataSourceManager;
    }

    @Override
    public void init() throws Throwable {
        if (transactionManager instanceof JtaTransactionManager) {
            delegate = ((JtaTransactionManager) transactionManager).getTransactionManager();
        } else {
            throw new IllegalStateException("Injected transaction manager is not of type JtaTransactionManager but "+ transactionManager.getClass().getName());
        }
    }

    @Override
    public void doRecovery() throws Throwable
    {
        TransactionResourceManager trm = new TransactionResourceManager()
        {
            @Override
            public void returnXAResource( String rmName, XAResource rmXares )
            {
            }
        };

        try
        {
            for ( XaDataSource xaDs : xaDataSourceManager.getAllRegisteredDataSources() )
            {
                Current.getTransactionRecovery().registerResourceManager( xaDs.getName(),
                        xaDs.getXaConnection().getXaResource(), xaDs.getName(), trm );
            }
            Current.getTransactionRecovery().startResourceManagerRecovery();
        }
        catch ( XAException e )
        {
            throw new Error( "Error registering xa datasource", e );
        }
    }

    @Override
    public TransactionState getTransactionState() {
        try
        {
            TransactionState state = states.get( getTransaction() );
            return state != null ? state : TransactionState.NO_STATE;
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public int getEventIdentifier() {
        return 0;
    }

    @Override
    public void start() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        states.clear();
    }

    public void begin() throws NotSupportedException, SystemException
    {
        delegate.begin();
        Transaction tx = getTransaction();
        states.put(tx, stateFactory.create(tx));
//        kernelTransactions.put( tx, kernelAPI.newTransaction() );
    }

    public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException
    {
        Transaction tx = getTransaction();
        delegate.commit();
        states.remove(tx);
    }

    public int getStatus() throws SystemException
    {
        return delegate.getStatus();
    }

    public Transaction getTransaction() throws SystemException
    {
        return delegate.getTransaction();
    }

    public void resume( Transaction tobj ) throws InvalidTransactionException,
            IllegalStateException, SystemException
    {
        delegate.resume( tobj );
    }

    public void rollback() throws IllegalStateException, SecurityException,
            SystemException
    {
        Transaction tx = getTransaction();
        delegate.rollback();
        states.remove(tx);
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException
    {
        delegate.setRollbackOnly();
    }

    public void setTransactionTimeout( int seconds ) throws SystemException
    {
        delegate.setTransactionTimeout( seconds );
    }

    public Transaction suspend() throws SystemException
    {
        return delegate.suspend();
    }

    @Override
    public void stop()
    {
        // Currently a no-op
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

//    @Override
//    public void setKernel(KernelAPI kernelAPI) {
//        this.kernelAPI = kernelAPI;
//    }
//
//    @Override
//    public KernelTransaction getKernelTransaction()
//    {
//        Transaction transaction;
//        try
//        {
//            transaction = getTransaction();
//        }
//        catch ( SystemException e )
//        {
//            return null;
//        }
//        return kernelTransactions.get( transaction );
//    }
}
