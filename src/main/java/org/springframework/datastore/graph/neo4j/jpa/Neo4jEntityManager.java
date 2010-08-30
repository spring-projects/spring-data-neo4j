package org.springframework.datastore.graph.neo4j.jpa;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.persistence.support.EntityInstantiator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.*;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.transaction.*;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 20.08.2010
 *        TODO Relationships
 */
//@Service
@Transactional
@Configurable
public class Neo4jEntityManager implements EntityManager {
    GraphDatabaseService graphDatabaseService;
    EntityInstantiator<NodeBacked, Node> nodeInstantiator;
    private PersistenceUnitInfo info;

    private Map params;
    private volatile boolean closed;
    private final FinderFactory finderFactory;

    public Neo4jEntityManager(final GraphDatabaseService graphDatabaseService, final EntityInstantiator<NodeBacked, Node> nodeInstantiator, PersistenceUnitInfo info, Map params) {
        this.graphDatabaseService = graphDatabaseService;
        this.nodeInstantiator = nodeInstantiator;
        this.info = info;
        this.params = params;
        finderFactory = new FinderFactory(graphDatabaseService, nodeInstantiator);
    }

    public Neo4jEntityManager() {
        finderFactory = new FinderFactory(graphDatabaseService, nodeInstantiator);
    }

    private Node nodeFor(final Object entity) {
        checkClosed();
        if (!(entity instanceof NodeBacked)) throw new IllegalArgumentException("Not a nodebacked entity " + entity);
        final Node node = ((NodeBacked) entity).getUnderlyingNode();
        if (node == null) throw new IllegalArgumentException("Node of entity " + entity + " is null");
        return node;
    }

    @Override
    public void persist(final Object entity) {
        checkClosed();
        final Transaction tx = graphDatabaseService.beginTx();
        try {

            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Override
    public <T> T merge(final T entity) {
        checkClosed();
        return entity;
    }

    @Override
    public void remove(final Object entity) {
        final Node node = nodeFor(entity);
        for (final Relationship r : node.getRelationships()) {
        	r.delete();
        }
		node.delete();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T find(final Class<T> entityClass, final Object primaryKey) {
        checkClosed();
        if (!NodeBacked.class.isAssignableFrom(entityClass))
            throw new IllegalArgumentException("Not a nodebacked entity type " + entityClass);
        if (!(primaryKey instanceof Number))
            throw new IllegalArgumentException("Primary Key for entity type " + entityClass + " is not a number " + primaryKey);
        final Node node = graphDatabaseService.getNodeById(((Number) primaryKey).longValue());
        return (T) nodeInstantiator.createEntityFromState(node, (Class<? extends NodeBacked>) entityClass);
    }

    @Override
    public <T> T getReference(final Class<T> entityClass, final Object primaryKey) {
        return find(entityClass, primaryKey);
    }

    @Override
    public void flush() {
        checkClosed();
    }

    @Override
    public void setFlushMode(final FlushModeType flushMode) {
        checkClosed();
    }

    @Override
    public FlushModeType getFlushMode() {
        checkClosed();
        return FlushModeType.AUTO;
    }

    @Override
    public void lock(final Object entity, final LockModeType lockMode) {
        nodeFor(entity);
    }

    @Override
    public void refresh(final Object entity) {
        nodeFor(entity);
        // todo NodeBacked.refresh -> discard dirty
    }

    /**
     * Clear the persistence context, causing all managed entities to become detached.
     */
    @Override
    public void clear() {
        checkClosed();
    }

    @Override
    public boolean contains(final Object entity) {
        checkClosed();
        try {
            return nodeFor(entity) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /*
    TODO Gremlin
     */

    @Override
    public Query createQuery(final String qlString) {
        checkClosed();
        return new Neo4JQuery(qlString, finderFactory,info);
    }

    /*
    TODO Gremlin
     */

    @Override
    public Query createNamedQuery(final String name) {
        checkClosed();
        throw new UnsupportedOperationException();
    }

    /*
    TODO Gremlin
     */

    @Override
    public Query createNativeQuery(final String sqlString) {
        checkClosed();
        throw new UnsupportedOperationException();
    }

    /*
    TODO Gremlin
     */

    @Override
    public Query createNativeQuery(final String sqlString, final Class resultClass) {
        checkClosed();
        throw new UnsupportedOperationException();
    }

    /*
    TODO Gremlin
     */

    @Override
    public Query createNativeQuery(final String sqlString, final String resultSetMapping) {
        checkClosed();
        throw new UnsupportedOperationException();
    }

    /*
    TODO Johan
     */

    @Override
    public void joinTransaction() {
        checkClosed();
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getDelegate() {
        checkClosed();
        return graphDatabaseService;
    }

    @Override
    public void close() {
        checkClosed();
        this.closed = true;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    private void checkClosed() {
        if (closed) throw new IllegalStateException("EntityManager is closed");
    }

    @Override
    public EntityTransaction getTransaction() {
        final TransactionManager transactionManager = ((EmbeddedGraphDatabase) graphDatabaseService).getConfig().getTxModule().getTxManager();
        return new Neo4jEntityTransaction(transactionManager);
    }


}
