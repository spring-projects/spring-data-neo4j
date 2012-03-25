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

import org.neo4j.kernel.impl.transaction.AbstractTransactionManager;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.*;

/**
 * @author Chris Gioran
 */
@Configurable
class SpringServiceImpl extends AbstractTransactionManager
{
    @Autowired
    private JtaTransactionManager jtaTransactionManager;

    private TransactionManager delegate;

    SpringServiceImpl()
    {
    }

    @Override
    public void init() throws Throwable {
        delegate = jtaTransactionManager.getTransactionManager();
    }

    @Override
    public void start() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {

    }

    public void begin() throws NotSupportedException, SystemException
    {
        delegate.begin();
    }

    public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException
    {
        delegate.commit();
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
        delegate.rollback();
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
}
