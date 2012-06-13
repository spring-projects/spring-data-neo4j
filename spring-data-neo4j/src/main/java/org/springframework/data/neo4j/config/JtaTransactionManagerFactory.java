package org.springframework.data.neo4j.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;

public class JtaTransactionManagerFactory
{
    public static JtaTransactionManager create( GraphDatabaseService gds )
    {
        JtaTransactionManager jtaTm = new JtaTransactionManager();
        Object[] transactionManagerAndUserTransaction = createTransactionManagerAndUserTransaction( gds );
        jtaTm.setTransactionManager( (TransactionManager) transactionManagerAndUserTransaction[0] );
        jtaTm.setUserTransaction( (UserTransaction) transactionManagerAndUserTransaction[1] );
        return jtaTm;
    }

    private static Object[] createTransactionManagerAndUserTransaction( GraphDatabaseService gds )
    {
        if ( onePointEight() )
        {
            return new Object[]{createTransactionManagerForOnePointEight( gds ),
                    createUserTransactionForOnePointEight( gds )};
        }
        if ( onePointSeven( gds ) )
        {
            return new Object[]{createTransactionManagerForOnePointSeven( gds ),
                    createUserTransactionForOnePointSeven( gds )};
        }
        if ( onePointSix( gds ) )
        {
            return new Object[]{createTransactionManagerForOnePointSix( gds ), createUserTransactionForOnePointSix(
                    gds )};
        }

        TransactionManager transactionManager = new NullTransactionManager();
        UserTransaction userTransaction = new UserTransactionAdapter( transactionManager );
        return new Object[]{transactionManager, userTransaction};
    }

    private static boolean onePointSix( GraphDatabaseService gds )
    {
        try
        {
            Class<?> possibleConstructorParameterClass = Class.forName( "org.neo4j.graphdb.GraphDatabaseService" );

            UserTransactionImpl.class.getDeclaredConstructor( possibleConstructorParameterClass );

            return AbstractGraphDatabase.class.isInstance( gds );
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
        catch ( NoSuchMethodException e )
        {
            return false;
        }
    }

    private static TransactionManager createTransactionManagerForOnePointSix( GraphDatabaseService gds )
    {
        return createDynamically( gds, SpringTransactionManager.class, "org.neo4j.graphdb.GraphDatabaseService" );
    }

    private static UserTransaction createUserTransactionForOnePointSix( GraphDatabaseService gds )
    {
        return createDynamically( gds, UserTransactionImpl.class, "org.neo4j.graphdb.GraphDatabaseService" );
    }

    private static boolean onePointSeven( GraphDatabaseService gds )
    {
        try
        {
            UserTransactionImpl.class.getDeclaredConstructor( TransactionManager.class );
            Class<?> gdaClass = Class.forName( "org.neo4j.kernel.GraphDatabaseAPI" );
            gds.getClass().getMethod( "getTxManager" );
            return gdaClass.isInstance( gds );
        }
        catch ( NoSuchMethodException e )
        {
            return false;
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
    }

    private static TransactionManager createTransactionManagerForOnePointSeven( GraphDatabaseService gds )
    {
        return createDynamically( gds, SpringTransactionManager.class, "org.neo4j.graphdb.GraphDatabaseService" );
    }

    private static UserTransaction createUserTransactionForOnePointSeven( GraphDatabaseService gds )
    {
        try
        {
            Method getTxManagerMethod = gds.getClass().getMethod( "getTxManager" );

            TransactionManager txManager = (TransactionManager) getTxManagerMethod.invoke( gds );

            return createDynamically( txManager, UserTransactionImpl.class, "javax.transaction.TransactionManager" );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static boolean onePointEight()
    {
        return UserTransactionImpl.class.getDeclaredConstructors().length == 1;
    }

    private static TransactionManager createTransactionManagerForOnePointEight( GraphDatabaseService gds )
    {
        return createDynamically( gds, SpringTransactionManager.class, "org.neo4j.kernel.GraphDatabaseAPI" );
    }

    private static UserTransaction createUserTransactionForOnePointEight( GraphDatabaseService gds )
    {
        return createDynamically( gds, UserTransactionImpl.class, "org.neo4j.kernel.GraphDatabaseAPI" );
    }

    private static <T> T createDynamically( Object parameter, Class<T> objectClass, String parameterClassName )
    {
        try
        {
            Class<?> parameterClass = Class.forName( parameterClassName );

            return objectClass.getDeclaredConstructor( parameterClass ).newInstance( parameter );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( e );
        }
        catch ( InstantiationException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( e );
        }
    }

}
