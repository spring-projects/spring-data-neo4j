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

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;

public class JtaTransactionManagerFactoryBean implements FactoryBean<JtaTransactionManager>
{
    private final JtaTransactionManager jtaTransactionManager;

    public JtaTransactionManagerFactoryBean( GraphDatabaseService gds ) throws Exception
    {
        jtaTransactionManager = create( gds );
    }

    @Override
    public JtaTransactionManager getObject() throws Exception
    {
        return jtaTransactionManager;
    }

    @Override
    public Class<JtaTransactionManager> getObjectType()
    {
        return JtaTransactionManager.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    private JtaTransactionManager create( GraphDatabaseService gds ) throws Exception
    {
        if ( !(gds instanceof GraphDatabaseAPI) )
        {
            return createNullJtaTransactionManager();
        }

        try
        {
            return createJtaTransactionManager( gds );
        }
        catch ( NoSuchMethodException e )
        {
            return createJtaTransactionManagerForOnePointSeven( gds );
        }
    }

    private JtaTransactionManager createNullJtaTransactionManager()
    {
        TransactionManager transactionManager = new NullTransactionManager();
        UserTransaction userTransaction = new UserTransactionAdapter( transactionManager );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private JtaTransactionManager createJtaTransactionManagerForOnePointSeven( GraphDatabaseService gds ) throws Exception
    {
        TransactionManager transactionManager = createTransactionManagerForOnePointSeven( gds );
        UserTransaction userTransaction = createUserTransactionForOnePointSeven( gds );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private JtaTransactionManager createJtaTransactionManager( GraphDatabaseService gds ) throws Exception
    {
        TransactionManager transactionManager = createTransactionManagerForOnePointEight( gds );
        UserTransaction userTransaction = createUserTransactionForOnePointEight( gds );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private TransactionManager createTransactionManagerForOnePointSeven( GraphDatabaseService gds ) throws Exception
    {
        return createDynamically( SpringTransactionManager.class, GraphDatabaseService.class, gds );
    }

    private UserTransaction createUserTransactionForOnePointSeven( GraphDatabaseService gds ) throws Exception
    {
        TransactionManager txManager = ((GraphDatabaseAPI) gds).getDependencyResolver().resolveDependency(TransactionManager.class);
        return createDynamically( UserTransactionImpl.class, TransactionManager.class, txManager );
    }

    private TransactionManager createTransactionManagerForOnePointEight( GraphDatabaseService gds ) throws Exception
    {
        return createDynamically( SpringTransactionManager.class, GraphDatabaseAPI.class, gds );
    }

    private UserTransaction createUserTransactionForOnePointEight( GraphDatabaseService gds ) throws Exception
    {
        return createDynamically( UserTransactionImpl.class, GraphDatabaseAPI.class, gds );
    }

    private <T> T createDynamically( Class<T> requiredClass, Class<?> argumentClass, Object gds ) throws Exception
    {
        return requiredClass.getDeclaredConstructor( argumentClass ).newInstance( gds );
    }

}
