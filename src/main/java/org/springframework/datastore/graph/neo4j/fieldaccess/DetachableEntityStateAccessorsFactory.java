package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

public class DetachableEntityStateAccessorsFactory {
	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	public EntityStateAccessors<NodeBacked,Node> getEntityStateAccessors(final NodeBacked entity) {
		return new DetachableEntityStateAccessors<NodeBacked, Node>(
				new NodeEntityStateAccessors<NodeBacked>(null,entity,entity.getClass(), graphDatabaseContext));
	}
}
