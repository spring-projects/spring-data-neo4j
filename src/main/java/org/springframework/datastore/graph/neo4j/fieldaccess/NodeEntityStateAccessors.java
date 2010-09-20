package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.GraphBacked;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class NodeEntityStateAccessors<ENTITY extends NodeBacked> extends DefaultEntityStateAccessors<ENTITY, Node> {

    public NodeEntityStateAccessors(final Node underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext) {
        super(underlyingState, entity, type, graphDatabaseContext);
    }

    @Override
    public void createAndAssignState() {
		try {
            final Node node=graphDatabaseContext.createNode();
            setUnderlyingState(node);
			entity.setUnderlyingState(node);
			log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getUnderlyingState() +"]; Updating metamodel");
			graphDatabaseContext.postEntityCreation(entity);
		} catch(NotInTransactionException e) {
			throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
		}
    }
}
