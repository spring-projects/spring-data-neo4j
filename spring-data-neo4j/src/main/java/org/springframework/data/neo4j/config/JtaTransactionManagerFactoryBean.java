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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class JtaTransactionManagerFactoryBean implements FactoryBean<JtaTransactionManager>
{
    private final JtaTransactionManager jtaTransactionManager;

    public JtaTransactionManagerFactoryBean( GraphDatabaseService gds )
    {
        jtaTransactionManager = create( gds );
    }

    static Constructor springTxManagerConstructor;
    static {
        try {
            springTxManagerConstructor = Class.forName("org.neo4j.kernel.impl.transaction.SpringTransactionManager").getConstructor(GraphDatabaseAPI.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            springTxManagerConstructor = null;
        }
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
            if (springTxManagerConstructor==null) return createJtaTransactionManager(new  Neo4jEmbeddedTransactionManager(gds));
            try {
                return createJtaTransactionManager((TransactionManager)springTxManagerConstructor.newInstance(gds));
            } catch (Exception e) {
                // throw new RuntimeException(e);
            }
        return createNullJtaTransactionManager();
    }

    private JtaTransactionManager createJtaTransactionManager(GraphDatabase gdb) {
        return createJtaTransactionManager(gdb.getTransactionManager());
    }

    private JtaTransactionManager createJtaTransactionManager(TransactionManager transactionManager)
    {
        UserTransaction userTransaction = new UserTransactionAdapter( transactionManager );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }

    private JtaTransactionManager createNullJtaTransactionManager()
    {
        TransactionManager transactionManager = new NullTransactionManager();
        UserTransaction userTransaction = new UserTransactionAdapter( transactionManager );

        return new JtaTransactionManager( userTransaction, transactionManager );
    }
/*
    private JtaTransactionManager createJtaTransactionManager( GraphDatabaseService gds )
    {
        TransactionManager transactionManager = createTransactionManagerForOnePointEight( gds );
        UserTransaction userTransaction = createUserTransactionForOnePointEight( gds );

        return new JtaTransactionManager( userTransaction, transactionManager );
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

*/
    private  boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException cnfe) {
            return false;
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
