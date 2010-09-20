package org.springframework.datastore.graph.neo4j.spi.relationship;

import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.*;
import org.springframework.datastore.graph.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

public aspect Neo4jRelationshipBacking extends AbstractTypeAnnotatingMixinFields<GraphRelationship,RelationshipBacked> {
	
	//-------------------------------------------------------------------------
	// Configure aspect for whole system.
	// init() method can be invoked automatically if the aspect is a Spring
	// bean, or called in user code.
	//-------------------------------------------------------------------------
	// Aspect shared Neo4J Graph Database Service
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	public void init(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }
	
	// Introduced fields
	private Relationship RelationshipBacked.underlyingRelationship;
	
	public void RelationshipBacked.setUnderlyingState(Relationship r) {
		this.underlyingRelationship = r;
	}
	
	public Relationship RelationshipBacked.getUnderlyingState() {
		return underlyingRelationship;
	}
	public boolean RelationshipBacked.hasUnderlyingRelationship() {
		return underlyingRelationship!=null;
	}

	public Long RelationshipBacked.getId() {
        if (!hasUnderlyingRelationship()) return null;
		return underlyingRelationship.getId();
	}
	
	
	//-------------------------------------------------------------------------
	// Equals and hashCode for Neo4j entities.
	// Final to prevent overriding.
	//-------------------------------------------------------------------------
	// TODO could use template method for further checks if needed
	public final boolean RelationshipBacked.equals(Object obj) {
		if (obj instanceof RelationshipBacked) {
			return this.getUnderlyingState().equals(((RelationshipBacked) obj).getUnderlyingState());
		}
		return false;
	}
	
	public final int RelationshipBacked.hashCode() {
		return getUnderlyingState().hashCode();
	}


	Object around(RelationshipBacked entity) : entityFieldGet(entity) {	
		FieldSignature fieldSignature=(FieldSignature) thisJoinPoint.getSignature();
		Field f = fieldSignature.getField();
		
        if (Modifier.isTransient(f.getModifiers())) {
            return proceed(entity);
        }

		if (isStartNodeField(f)) {
			Node startNode = entity.getUnderlyingState().getStartNode();
			if (startNode == null) {
				return null;
			}
			return graphDatabaseContext.createEntityFromState(startNode, (Class<? extends NodeBacked>) f.getType());
		}
		if (isEndNodeField(f)) {
			Node endNode = entity.getUnderlyingState().getEndNode();
			if (endNode == null) {
				return null;
			}
			return graphDatabaseContext.createEntityFromState(endNode, (Class<? extends NodeBacked>) f.getType());
		}
		
//		TODO fix arrays, TODO serialize other types as byte[] or string (for indexing, querying) via Annotation
		if (isNeo4jPropertyType(f.getType()) || isDeserializableField(f)) {
			Relationship rel = entity.getUnderlyingState();
			if (rel == null) {
				throw new InvalidDataAccessApiUsageException("Please set start node and end node before reading from other fields.");
			}
			String propName = DelegatingFieldAccessorFactory.getNeo4jPropertyName(f);
			log.info("GET " + f + " <- Neo4J simple relationship property [" + propName + "]");
			return deserializePropertyValue(rel.getProperty(propName, null), f.getType());
		}
		
		return proceed(entity);
	}
	
	Object around(RelationshipBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
		try {
			FieldSignature fieldSignature = (FieldSignature) thisJoinPoint.getSignature();
			Field f = fieldSignature.getField();

			if (Modifier.isTransient(f.getModifiers())) {
				return proceed(entity, newVal);
			}

			if (isStartNodeField(f) || isEndNodeField(f)) {
				throw new InvalidDataAccessApiUsageException("Cannot change start node or end node of existing relationship.");
			}
			if (isNeo4jPropertyType(f.getType()) || isSerializableField(f)) {
				Relationship rel = entity.getUnderlyingState();
				if (rel == null) {
					throw new InvalidDataAccessApiUsageException("Please set start node and end node before assigning to other fields.");
				}
				String propName = DelegatingFieldAccessorFactory.getNeo4jPropertyName(f);
                if (newVal==null) {
                    entity.getUnderlyingState().removeProperty(propName);
                }
                else {
                    entity.getUnderlyingState().setProperty(propName, serializePropertyValue(newVal, f.getType()));
                }
				log.info("SET " + f + " -> Neo4J simple relationship property [" + propName + "] with value=[" + newVal + "]");
				return proceed(entity, newVal);
			}
			return proceed(entity, newVal);
		} catch(NotInTransactionException e) {
			throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
		}
	}

	private boolean isEndNodeField(Field f) {
		return f.isAnnotationPresent(GraphRelationshipEndNode.class);
	}

	private boolean isStartNodeField(Field f) {
		return f.isAnnotationPresent(GraphRelationshipStartNode.class);
	}

	private boolean isNeo4jPropertyType(Class<?> fieldType) {
		// todo: add array support
		return fieldType.isPrimitive()
	  		|| (fieldType.isArray() && !fieldType.getComponentType().isArray() && isNeo4jPropertyType(fieldType.getComponentType()))
	  		|| fieldType.equals(String.class)
	  		|| fieldType.equals(Character.class)
	  		|| fieldType.equals(Boolean.class)
	  		|| (fieldType.getName().startsWith("java.lang") && Number.class.isAssignableFrom(fieldType));
	}

	private boolean isSerializableField(Field field) {
		return !isRelationshipField(field) && graphDatabaseContext.canConvert(field.getType(), String.class);
	}

	private boolean isDeserializableField(Field field) {
		return !isRelationshipField(field) && graphDatabaseContext.canConvert(String.class, field.getType());
	}

	private Object serializePropertyValue(Object newVal, Class<?> fieldType) {
		if (isNeo4jPropertyType(fieldType)) {
			return newVal;
		}
		return graphDatabaseContext.convert(newVal, String.class);
	}

	private Object deserializePropertyValue(Object value, Class<?> fieldType) {
		if (isNeo4jPropertyType(fieldType)) {
			return value;
		}
		return graphDatabaseContext.convert(value, fieldType);
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


}
