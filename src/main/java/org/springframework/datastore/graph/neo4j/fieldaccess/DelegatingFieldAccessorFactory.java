package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.datastore.graph.api.*;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

public class DelegatingFieldAccessorFactory<T> implements FieldAccessorFactory<T> {
    private final GraphDatabaseContext graphDatabaseContext;

    public DelegatingFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public boolean accept(Field f) {
        return isSingleRelationshipField(f) || isOneToNRelationshipEntityField(f) || isReadOnlyOneToNRelationshipField(f) || isOneToNRelationshipField(f);
    }

    final Collection<FieldAccessorFactory<?>> fieldAccessorFactories = Arrays.<FieldAccessorFactory<?>>asList(
            NodePropertyFieldAccessor.factory(),
            ConvertingNodePropertyFieldAccessor.factory(),
            SingleRelationshipFieldAccessor.factory(),
            OneToNRelationshipFieldAccessor.factory(),
            ReadOnlyOneToNRelationshipFieldAccessor.factory(),
            OneToNRelationshipEntityFieldAccessor.factory()
            );
    final Collection<FieldAccessorListenerFactory<?>> fieldAccessorListenerFactories = Arrays.<FieldAccessorListenerFactory<?>>asList(
            IndexingNodePropertyFieldAccessorListener.factory()
            );

    public FieldAccessor forField(Field field) {
        if (Modifier.isTransient(field.getModifiers())) return null;
	    for (FieldAccessorFactory<?> fieldAccessorFactory : fieldAccessorFactories) {
		    if (fieldAccessorFactory.accept(field)) {
			    System.out.println("Factory " + fieldAccessorFactory + " used for field: " + field);
			    return fieldAccessorFactory.forField(field);
		    }
	    }
		throw new IllegalArgumentException("Not a Neo4j relationship field: " + field);
	}


    private Class<? extends NodeBacked> targetFrom(Field field) {
        return (Class<? extends NodeBacked>) field.getType();
    }

    private Class<? extends NodeBacked> targetFrom(GraphEntityRelationship relAnnotation) {
        return relAnnotation.elementClass();
    }

    private Direction dirFrom(GraphEntityRelationship relAnnotation) {
        return relAnnotation.direction().toNeo4jDir();
    }

    private DynamicRelationshipType typeFrom(Field field) {
        return DynamicRelationshipType.withName(getNeo4jPropertyName(field));
    }

    private Class<? extends RelationshipBacked> targetFrom(GraphEntityRelationshipEntity relEntityAnnotation) {
        return relEntityAnnotation.elementClass();
    }

    private Direction dirFrom(GraphEntityRelationshipEntity relEntityAnnotation) {
        return relEntityAnnotation.direction().toNeo4jDir();
    }
    private DynamicRelationshipType typeFrom(GraphEntityRelationshipEntity relEntityAnnotation) {
        return DynamicRelationshipType.withName(relEntityAnnotation.type());
    }

    private DynamicRelationshipType typeFrom(GraphEntityRelationship relAnnotation) {
        return DynamicRelationshipType.withName(relAnnotation.type());
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
        if (useShortNames(entityClass)) return field.getName();
        return String.format("%s.%s", entityClass.getSimpleName(), field.getName());
    }

    private static boolean useShortNames(Class<?> entityClass) {
        final GraphEntity graphEntity = entityClass.getAnnotation(GraphEntity.class);
        if (graphEntity!=null) return graphEntity.useShortNames();
        final GraphRelationship graphRelationship = entityClass.getAnnotation(GraphRelationship.class);
        if (graphRelationship!=null) return graphRelationship.useShortNames();
        return false;
    }

    public List<FieldAccessor<T, ?>> listenersFor(Field field) {
        List<FieldAccessor<T,?>> result=new ArrayList<FieldAccessor<T,?>>();
        for (FieldAccessorListenerFactory<?> fieldAccessorListenerFactory : fieldAccessorListenerFactories) {
            if (fieldAccessorListenerFactory.accept(field)) {
                final FieldAccessor<T, ?> listener = (FieldAccessor<T, ?>) fieldAccessorListenerFactory.forField(field);
                result.add(listener);
            }
        }
        return result;
    }
}