package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class NodeEntityStateAccessors<ENTITY extends NodeBacked> extends DefaultEntityStateAccessors<ENTITY, Node> {

    private final GraphDatabaseContext graphDatabaseContext;

    public NodeEntityStateAccessors(final Node underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext, final NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory) {
        super(underlyingState, entity, type, nodeDelegatingFieldAccessorFactory);
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public void createAndAssignState() {
        if (entity.getUnderlyingState()!=null) return;
        try {
            final Object id = getIdFromEntity();
            if (id instanceof Number) {
                final Node node = graphDatabaseContext.getNodeById(((Number) id).longValue());
                setUnderlyingState(node);
                entity.setUnderlyingState(node);
                if (log.isInfoEnabled())
                    log.info("Entity reattached " + entity.getClass() + "; used Node [" + entity.getUnderlyingState() + "];");
                return;
            }

            final Node node = graphDatabaseContext.createNode();
            setUnderlyingState(node);
            entity.setUnderlyingState(node);
            if (log.isInfoEnabled()) log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getUnderlyingState() + "]; Updating metamodel");
            graphDatabaseContext.postEntityCreation(entity);
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }
}
