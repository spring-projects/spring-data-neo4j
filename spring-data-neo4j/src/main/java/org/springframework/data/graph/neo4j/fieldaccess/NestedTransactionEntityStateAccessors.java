package org.springframework.data.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

public class NestedTransactionEntityStateAccessors<ENTITY extends GraphBacked<STATE>, STATE> implements
        EntityStateAccessors<ENTITY, STATE>
{
    protected final EntityStateAccessors<ENTITY, STATE> delegate;
    private final static Log log = LogFactory.getLog( NestedTransactionEntityStateAccessors.class );
    private GraphDatabaseContext graphDatabaseContext;

    public NestedTransactionEntityStateAccessors( final EntityStateAccessors<ENTITY, STATE> delegate,
                                                  GraphDatabaseContext graphDatabaseContext )
    {
        this.delegate = delegate;
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public ENTITY getEntity()
    {
        return delegate.getEntity();
    }

    public void setUnderlyingState( STATE state )
    {
        delegate.setUnderlyingState( state );
    }

    @Override
    public Object getValue( Field field )
    {
        return delegate.getValue( field );
    }

    @Override
    public boolean isWritable( Field field )
    {
        return delegate.isWritable( field );
    }

    @Override
    public Object setValue( Field field, Object newVal )
    {
        Transaction tx = graphDatabaseContext.beginTx();

        try
        {
            Object result = delegate.setValue( field, newVal );
            tx.success();
            return result;
        } finally
        {
            tx.finish();
        }
    }

    @Override
    public void createAndAssignState()
    {
        Transaction tx = graphDatabaseContext.beginTx();

        try
        {
            delegate.createAndAssignState();
            tx.success();
        } finally
        {
            tx.finish();
        }
    }

    @Override
    public boolean hasUnderlyingState()
    {
        return delegate.hasUnderlyingState();
    }

    @Override
    public STATE getUnderlyingState()
    {
        return delegate.getUnderlyingState();
    }
}