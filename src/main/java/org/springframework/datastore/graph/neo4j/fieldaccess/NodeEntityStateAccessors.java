package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class NodeEntityStateAccessors<ENTITY extends NodeBacked> extends DefaultEntityStateAccessors<ENTITY, Node> {

    private final GraphDatabaseContext graphDatabaseContext;

    public NodeEntityStateAccessors(final Node underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext) {
        super(underlyingState, entity, type, new DelegatingFieldAccessorFactory(graphDatabaseContext) {
            @Override
            protected Collection<FieldAccessorListenerFactory<?>> createListenerFactories() {
	            return Arrays.<FieldAccessorListenerFactory<?>>asList(
			            new IndexingNodePropertyFieldAccessorListenerFactory()
                );
            }

            @Override
            protected Collection<? extends FieldAccessorFactory<?>> createAccessorFactories() {
                return Arrays.<FieldAccessorFactory<?>>asList(
                        new IdFieldAccessorFactory(),
                        new TransientFieldAccessorFactory(),
                        new PropertyFieldAccessorFactory(),
                        new ConvertingNodePropertyFieldAccessorFactory(),
                        new SingleRelationshipFieldAccessorFactory(),
                        new OneToNRelationshipFieldAccessorFactory(),
                        new ReadOnlyOneToNRelationshipFieldAccessorFactory(),
                        new TraversalFieldAccessorFactory(),
                        new OneToNRelationshipEntityFieldAccessorFactory()
                );
            }
        });
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public void createAndAssignState() {
        if (entity.getUnderlyingState()!=null) return;
        try {
            final Node node = graphDatabaseContext.createNode();
            setUnderlyingState(node);
            entity.setUnderlyingState(node);
            log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getUnderlyingState() + "]; Updating metamodel");
            graphDatabaseContext.postEntityCreation(entity);
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }
}
