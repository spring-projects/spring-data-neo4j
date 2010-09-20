package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Relationship;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipEntityStateAccessors<ENTITY extends RelationshipBacked> extends DefaultEntityStateAccessors<ENTITY, Relationship> {

    public RelationshipEntityStateAccessors(final Relationship underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext) {
        super(underlyingState, entity, type, graphDatabaseContext);
    }

    @Override
    public void createAndAssignState() {
		try {
            final Relationship relationship=null; // TODO graphDatabaseContext.create();
            setUnderlyingState(relationship);
			entity.setUnderlyingState(relationship);
			log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getUnderlyingState() +"]; Updating metamodel");
		} catch(NotInTransactionException e) {
			throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
		}
    }
}
