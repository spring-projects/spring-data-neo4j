package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

public class OneToNRelationshipFieldAccessor implements FieldAccessor {

	private final RelationshipType type;
	private final Direction direction;
	private final Class<? extends NodeBacked> relatedType;
	private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

	public OneToNRelationshipFieldAccessor(RelationshipType type, Direction direction, Class<? extends NodeBacked> elementClass, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
		this.type = type;
		this.direction = direction;
		this.relatedType = elementClass;
		this.graphEntityInstantiator = graphEntityInstantiator;
	}

	public Object apply(final NodeBacked entity, final Object newVal) {
		Node entityNode = entity.getUnderlyingNode();
		
		Set<Node> newNodes=new HashSet<Node>();
		if (newVal != null) {
			if (!(newVal instanceof Set)) {
				throw new IllegalArgumentException("New value must be a Set, was: " + newVal.getClass());
			}
			Set<Object> set = (Set<Object>) newVal;
			for (Object obj : set) {
				if (!(obj instanceof NodeBacked)) {
					throw new IllegalArgumentException("New value elements must be NodeBacked.");
				}
				Node newNode=((NodeBacked) obj).getUnderlyingNode();
				if (entityNode.equals(newNode)) {
					throw new InvalidDataAccessApiUsageException("Cannot create circular reference.");
				}
				newNodes.add(newNode);
			}
		}
		for ( Relationship relationship : entityNode.getRelationships(type, direction) ) {
			if (!newNodes.remove(relationship.getOtherNode(entityNode)))
				relationship.delete();
		}
		if (newVal == null) {
			return null;
		}
		
		for (Node newNode : newNodes) {
			switch(direction) {
				case OUTGOING : entityNode.createRelationshipTo(newNode, type); break;
				case INCOMING : newNode.createRelationshipTo(entityNode, type); break;
				default : throw new IllegalArgumentException("invalid direction " + direction); 
			}
		}
		return new ManagedFieldAccessorSet<NodeBacked>(entity, newVal, this);
	}
	
	@Override
	public Object readObject(NodeBacked entity) {
		Node entityNode = entity.getUnderlyingNode();
		if (entityNode == null) {
			throw new IllegalStateException("Entity must have a backing Node");
		}
		Set<NodeBacked> result = new HashSet<NodeBacked>();
		for (Relationship rel : entityNode.getRelationships(type, direction)) {
			result.add(graphEntityInstantiator.createEntityFromState(rel.getOtherNode(entityNode), relatedType));
		}
		return new ManagedFieldAccessorSet<NodeBacked>(entity, result, this); 
	}
	
}

	