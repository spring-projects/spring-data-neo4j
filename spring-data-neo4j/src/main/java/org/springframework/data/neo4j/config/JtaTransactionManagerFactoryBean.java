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
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jEmbeddedTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;

import java.lang.reflect.InvocationTargetException;

public class JtaTransactionManagerFactoryBean implements FactoryBean<JtaTransactionManager>
{
    private final JtaTransactionManager jtaTransactionManager;

    public JtaTransactionManagerFactoryBean( GraphDatabaseService gds )
    {
        jtaTransactionManager = create( gds );
    }

    @Override
    public JtaTransactionManager getObject()
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

    private JtaTransactionManager create( GraphDatabaseService gds )
    {
        if  ( gds instanceof GraphDatabase) {
            return createJtaTransactionManager( (GraphDatabase)gds );
        }
        if ( gds instanceof GraphDatabaseAPI)
            try
            {
                return createJtaTransactionManager( gds );
            }
            catch ( RuntimeException e )
            {
                if (e.getCause() instanceof NoSuchMethodException)
                    return createJtaTransactionManagerForOnePointSeven( gds );
            }

        return createEmbeddedJtaTransactionManager(gds);
    }

    private JtaTransactionManager createJtaTransactionManager(GraphDatabase gdb)
    {
        TransactionManager transactionManager = gdb.getTransactionManager();
        UserTransaction userTransaction = new UserTransactionAdapter( transactionManager );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private JtaTransactionManager createNullJtaTransactionManager()
    {
        TransactionManager transactionManager = new NullTransactionManager();
        UserTransaction userTransaction = new UserTransactionAdapter( transactionManager );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private JtaTransactionManager createEmbeddedJtaTransactionManager(GraphDatabaseService gds)
    {
        TransactionManager transactionManager = new Neo4jEmbeddedTransactionManager(gds);
        UserTransaction userTransaction = new UserTransactionAdapter( transactionManager );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private JtaTransactionManager createJtaTransactionManagerForOnePointSeven( GraphDatabaseService gds )
    {
        TransactionManager transactionManager = createTransactionManagerForOnePointSeven( gds );
        UserTransaction userTransaction = createUserTransactionForOnePointSeven( gds );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private JtaTransactionManager createJtaTransactionManager( GraphDatabaseService gds )
    {
        TransactionManager transactionManager = createTransactionManagerForOnePointEight( gds );
        UserTransaction userTransaction = createUserTransactionForOnePointEight( gds );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private TransactionManager createTransactionManagerForOnePointSeven( GraphDatabaseService gds )
    {
        return createDynamically( this.<TransactionManager>classFor("org.neo4j.kernel.impl.transaction.SpringTransactionManager"), GraphDatabaseService.class, gds );
    }

    private UserTransaction createUserTransactionForOnePointSeven( GraphDatabaseService gds )
    {
        TransactionManager txManager = ((GraphDatabaseAPI) gds).getDependencyResolver().resolveDependency(TransactionManager.class);
        return createDynamically(this.<UserTransaction>classFor("org.neo4j.kernel.impl.transaction.UserTransactionImpl"), TransactionManager.class, txManager);
    }

    private TransactionManager createTransactionManagerForOnePointEight( GraphDatabaseService gds )
    {
        return createDynamically( this.<TransactionManager>classFor("org.neo4j.kernel.impl.transaction.SpringTransactionManager"), GraphDatabaseAPI.class, gds );
    }

    private UserTransaction createUserTransactionForOnePointEight( GraphDatabaseService gds )
    {
        return createDynamically( this.<UserTransaction>classFor("org.neo4j.kernel.impl.transaction.UserTransactionImpl"), GraphDatabaseAPI.class, gds );
    }

    private <T> Class<T> classFor(String name) {
        try {
            return (Class<T>) Class.forName(name);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Class not found",cnfe);
        }
    }
    private <T> T createDynamically( Class<T> requiredClass, Class<?> argumentClass, Object gds )
    {
        try {
            return requiredClass.getDeclaredConstructor( argumentClass ).newInstance( gds );
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Error accessing constructor of class "+requiredClass+ " for parameter type "+argumentClass,e);
        }
    }
}
