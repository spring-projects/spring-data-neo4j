package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import static org.springframework.datastore.graph.neo4j.fieldaccess.PartialNodeEntityStateAccessors.getId;

public class NodeEntityStateAccessorsFactory {
	@Autowired
	private GraphDatabaseContext graphDatabaseContext;
    @Autowired
    private NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory;

	public EntityStateAccessors<NodeBacked,Node> getEntityStateAccessors(final NodeBacked entity) {
        final NodeEntity graphEntityAnnotation = entity.getClass().getAnnotation(NodeEntity.class);
        if (graphEntityAnnotation!=null && graphEntityAnnotation.partial()) {
            return new DetachableEntityStateAccessors<NodeBacked, Node>(
                    new PartialNodeEntityStateAccessors<NodeBacked>(null, entity, entity.getClass(), graphDatabaseContext), graphDatabaseContext) {
                @Override
                protected boolean transactionIsRunning() {
                    return super.transactionIsRunning() && getId(entity, entity.getClass()) != null;
                }
            };
        } else {
            return new DetachableEntityStateAccessors<NodeBacked, Node>(
                    new NodeEntityStateAccessors<NodeBacked>(null,entity,entity.getClass(), graphDatabaseContext, nodeDelegatingFieldAccessorFactory),graphDatabaseContext);
        }
    }
}
