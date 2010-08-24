package org.springframework.persistence.graph.neo4j;

import java.lang.reflect.Field;
import java.util.Collection;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.persistence.graph.Graph;
import org.springframework.persistence.support.EntityInstantiator;

public class FieldAccessorFactory {
	private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
	private final EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator;

	public FieldAccessorFactory(EntityInstantiator<NodeBacked,Node> graphEntityInstantiator, EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
		this.graphEntityInstantiator = graphEntityInstantiator;
		this.relationshipEntityInstantiator = relationshipEntityInstantiator;
	}
	
	public FieldAccessor forField(Field field) {
		Graph.Entity.Relationship relAnnotation = field.getAnnotation(Graph.Entity.Relationship.class);
		if (isSingleRelationshipField(field)) {
			Class<? extends NodeBacked> relatedType = (Class<? extends NodeBacked>) field.getType();
			if (relAnnotation != null) {
				return new SingleRelationshipFieldAccessor(DynamicRelationshipType.withName(relAnnotation.type()), 
						relAnnotation.direction().toNeo4jDir(), relatedType, graphEntityInstantiator);
			}
			return new SingleRelationshipFieldAccessor(DynamicRelationshipType.withName(getNeo4jPropertyName(field)), 
					Direction.OUTGOING, relatedType, graphEntityInstantiator);
		}
		if (isOneToNRelationshipField(field)) {
			return new OneToNRelationshipFieldAccessor(DynamicRelationshipType.withName(relAnnotation.type()), 
					relAnnotation.direction().toNeo4jDir(), relAnnotation.elementClass(), graphEntityInstantiator);
		}
		if (isReadOnlyOneToNRelationshipField(field)) {
			return new ReadOnlyOneToNRelationshipFieldAccessor(DynamicRelationshipType.withName(relAnnotation.type()), 
					relAnnotation.direction().toNeo4jDir(), relAnnotation.elementClass(), graphEntityInstantiator);
		}
		if (isOneToNRelationshipEntityField(field)) {
			Graph.Entity.RelationshipEntity relEntityAnnotation = field.getAnnotation(Graph.Entity.RelationshipEntity.class);
			return new OneToNRelationshipEntityFieldAccessor(DynamicRelationshipType.withName(relEntityAnnotation.type()), 
					relEntityAnnotation.direction().toNeo4jDir(), relEntityAnnotation.elementClass(), relationshipEntityInstantiator);
		}
		throw new IllegalArgumentException("Not a Neo4j relationship field: " + field);
	}

	private static boolean isSingleRelationshipField(Field f) {
		return NodeBacked.class.isAssignableFrom(f.getType());
	}
	
	private static boolean isOneToNRelationshipField(Field f) {
		if (!Collection.class.isAssignableFrom(f.getType())) return false;
		Graph.Entity.Relationship relAnnotation = f.getAnnotation(Graph.Entity.Relationship.class);
		return relAnnotation != null &&  NodeBacked.class.isAssignableFrom(relAnnotation.elementClass()) && !relAnnotation.elementClass().equals(NodeBacked.class);
	}

	private static boolean isReadOnlyOneToNRelationshipField(Field f) {
		Graph.Entity.Relationship relAnnotation = f.getAnnotation(Graph.Entity.Relationship.class);
		return Iterable.class.equals(f.getType()) 
			&& relAnnotation != null 
			&& !NodeBacked.class.equals(relAnnotation.elementClass());
	}

	private static boolean isOneToNRelationshipEntityField(Field f) {
		Graph.Entity.RelationshipEntity relEntityAnnotation = f.getAnnotation(Graph.Entity.RelationshipEntity.class);
		return Iterable.class.isAssignableFrom(f.getType()) 
			&& relEntityAnnotation != null 
			&& !RelationshipBacked.class.equals(relEntityAnnotation.elementClass());
	}

	public static String getNeo4jPropertyName(Field field) {
		return String.format("%s.%s",field.getDeclaringClass().getSimpleName(),field.getName());
	}
}