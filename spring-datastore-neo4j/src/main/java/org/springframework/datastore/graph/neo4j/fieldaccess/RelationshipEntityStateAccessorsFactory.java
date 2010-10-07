package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

public class RelationshipEntityStateAccessorsFactory {
	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	public EntityStateAccessors<RelationshipBacked, Relationship> getEntityStateAccessors(final RelationshipBacked entity) {
		return new RelationshipEntityStateAccessors<RelationshipBacked>(null,entity,entity.getClass(), graphDatabaseContext);
	}
}
