package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

public class SingleRelationshipFieldAccessor implements FieldAccessor {

	private final RelationshipType type;
	private final Direction direction;
	private final Class<? extends NodeBacked> relatedType;
	private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
	
	public SingleRelationshipFieldAccessor(RelationshipType type, Direction direction, Class<? extends NodeBacked> clazz, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
		this.type = type;
		this.direction = direction;
		this.relatedType = clazz;
		this.graphEntityInstantiator = graphEntityInstantiator;
	}

	public Object apply(NodeBacked entity, Object newVal) {
		if (newVal != null && !(newVal instanceof NodeBacked)) {
			throw new IllegalArgumentException("New value must be NodeBacked.");
		}
		Node entityNode = entity.getUnderlyingNode();
		for ( Relationship relationship : entityNode.getRelationships(type, direction) ) {
			relationship.delete();
		}
		if (newVal == null) {
			return null;
		}
		Node targetNode = ((NodeBacked) newVal).getUnderlyingNode();
		if (entityNode.equals(targetNode)) {
			throw new InvalidDataAccessApiUsageException("Cannot create circular reference.");
		}
		switch(direction) {
			case OUTGOING : entityNode.createRelationshipTo(targetNode, type); break;
			case INCOMING : targetNode.createRelationshipTo(entityNode, type); break;
			default : throw new IllegalArgumentException("invalid direction " + direction); 
		}
		return newVal;
	}

	@Override
	public Object readObject(NodeBacked entity) {
		Node entityNode = entity.getUnderlyingNode();
		if (entityNode == null) {
			throw new IllegalStateException("Entity must have a backing Node");
		}
		Relationship singleRelationship = entityNode.getSingleRelationship(type, direction);
		
		if (singleRelationship == null) {
			return null;
		}
		Node targetNode = singleRelationship.getOtherNode(entityNode);
		return graphEntityInstantiator.createEntityFromState(targetNode, relatedType);
	}
	
}