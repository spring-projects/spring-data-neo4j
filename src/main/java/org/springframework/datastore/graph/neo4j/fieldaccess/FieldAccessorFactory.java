package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.datastore.graph.api.*;
import org.springframework.persistence.support.EntityInstantiator;

public class FieldAccessorFactory {
	private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
	private final EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator;

	public FieldAccessorFactory(EntityInstantiator<NodeBacked,Node> graphEntityInstantiator, EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
		this.graphEntityInstantiator = graphEntityInstantiator;
		this.relationshipEntityInstantiator = relationshipEntityInstantiator;
	}
	
	public FieldAccessor forField(Field field) {
        if (Modifier.isTransient(field.getModifiers())) return null;
		GraphEntityRelationship relAnnotation = field.getAnnotation(GraphEntityRelationship.class);
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
			GraphEntityRelationshipEntity relEntityAnnotation = field.getAnnotation(GraphEntityRelationshipEntity.class);
			return new OneToNRelationshipEntityFieldAccessor(DynamicRelationshipType.withName(relEntityAnnotation.type()), 
					relEntityAnnotation.direction().toNeo4jDir(), relEntityAnnotation.elementClass(), relationshipEntityInstantiator);
		}
		throw new IllegalArgumentException("Not a Neo4j relationship field: " + field);
	}
	
	public static boolean isRelationshipField(Field f) {
		return isSingleRelationshipField(f) 
			|| isOneToNRelationshipField(f)
			|| isOneToNRelationshipEntityField(f)
			|| isReadOnlyOneToNRelationshipField(f);
	}

	private static boolean isSingleRelationshipField(Field f) {
		return NodeBacked.class.isAssignableFrom(f.getType());
	}
	
	private static boolean isOneToNRelationshipField(Field f) {
		if (!Collection.class.isAssignableFrom(f.getType())) return false;
		GraphEntityRelationship relAnnotation = f.getAnnotation(GraphEntityRelationship.class);
		return relAnnotation != null &&  NodeBacked.class.isAssignableFrom(relAnnotation.elementClass()) && !relAnnotation.elementClass().equals(NodeBacked.class);
	}

	private static boolean isReadOnlyOneToNRelationshipField(Field f) {
		GraphEntityRelationship relAnnotation = f.getAnnotation(GraphEntityRelationship.class);
		return Iterable.class.equals(f.getType()) 
			&& relAnnotation != null 
			&& !NodeBacked.class.equals(relAnnotation.elementClass());
	}

	private static boolean isOneToNRelationshipEntityField(Field f) {
		GraphEntityRelationshipEntity relEntityAnnotation = f.getAnnotation(GraphEntityRelationshipEntity.class);
		return Iterable.class.isAssignableFrom(f.getType()) 
			&& relEntityAnnotation != null 
			&& !RelationshipBacked.class.equals(relEntityAnnotation.elementClass());
	}

	public static String getNeo4jPropertyName(Field field) {
        final Class<?> entityClass = field.getDeclaringClass();
        return useShortNames(entityClass) ? field.getName() : String.format("%s.%s", entityClass.getSimpleName(),field.getName());
	}

    private static boolean useShortNames(Class<?> entityClass) {
        final GraphEntity graphEntity = entityClass.getAnnotation(GraphEntity.class);
        if (graphEntity!=null) return graphEntity.useShortNames();
        final GraphRelationship graphRelationship = entityClass.getAnnotation(GraphRelationship.class);
        if (graphRelationship!=null) return graphRelationship.useShortNames();
        return false;
    }
}