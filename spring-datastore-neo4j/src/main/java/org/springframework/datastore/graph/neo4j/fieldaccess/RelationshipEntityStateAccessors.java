package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Relationship;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.GraphInactive;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipEntityStateAccessors<ENTITY extends RelationshipBacked> extends DefaultEntityStateAccessors<ENTITY, Relationship> {

    private final GraphDatabaseContext graphDatabaseContext;

    public RelationshipEntityStateAccessors(final Relationship underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext) {
        super(underlyingState, entity, type, new DelegatingFieldAccessorFactory(graphDatabaseContext) {
            @Override
            protected Collection<FieldAccessorListenerFactory<?>> createListenerFactories() {
                return Collections.emptyList();
            }

            @Override
            protected Collection<? extends FieldAccessorFactory<?>> createAccessorFactories() {
                return Arrays.<FieldAccessorFactory<?>>asList(
                        new TransientFieldAccessorFactory(),
                        new RelationshipNodeFieldAccessorFactory(),
                        new PropertyFieldAccessorFactory(),
                        new ConvertingNodePropertyFieldAccessorFactory()
                );
            }
        });
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public void createAndAssignState() {
        if (entity.getUnderlyingState()!=null) return;
        try {
            final Object id = getIdFromEntity();
            if (id instanceof Number) {
                final Relationship relationship = graphDatabaseContext.getRelationshipById(((Number) id).longValue());
                setUnderlyingState(relationship);
                entity.setUnderlyingState(relationship);
                if (log.isInfoEnabled())
                    log.info("Entity reattached " + entity.getClass() + "; used Relationship [" + entity.getUnderlyingState() + "];");
                return;
            }

            final Relationship relationship = null; // TODO graphDatabaseContext.create();
            setUnderlyingState(relationship);
            entity.setUnderlyingState(relationship);
            if (log.isInfoEnabled()) log.info("User-defined constructor called on class " + entity.getClass() + "; created Relationship [" + entity.getUnderlyingState() + "]; Updating metamodel");
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }
}
