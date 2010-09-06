package org.springframework.datastore.graph.neo4j.spi.relationship;

import java.lang.reflect.Field;

import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.*;

import org.springframework.datastore.graph.neo4j.fieldaccess.FieldAccessorFactory;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;
import org.springframework.persistence.support.EntityInstantiator;

/**
 * Aspect to turn an object annotated with GraphEntity into a graph entity using Neo4J.
 * Delegates all field access (except for fields assumed to be transient)
 * to an underlying Neo4 graph node.
 * 
 * @author Rod Johnson
 */
public aspect Neo4jRelationshipBacking extends AbstractTypeAnnotatingMixinFields<GraphRelationship,RelationshipBacked> {
	
	//-------------------------------------------------------------------------
	// Configure aspect for whole system.
	// init() method can be invoked automatically if the aspect is a Spring
	// bean, or called in user code.
	//-------------------------------------------------------------------------
	// Aspect shared Neo4J Graph Database Service
	private EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
	
	@Autowired
	public void setEntityInstantiator(EntityInstantiator<NodeBacked, Node> entityInstantiator) {
		this.graphEntityInstantiator = entityInstantiator;
	}
	
	// Introduced fields
	private Relationship RelationshipBacked.underlyingRelationship;
	
	public void RelationshipBacked.setUnderlyingRelationship(Relationship r) {
		this.underlyingRelationship = r;
	}
	
	public Relationship RelationshipBacked.getUnderlyingRelationship() {
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
			return this.getUnderlyingRelationship().equals(((RelationshipBacked) obj).getUnderlyingRelationship());
		}
		return false;
	}
	
	public final int RelationshipBacked.hashCode() {
		return getUnderlyingRelationship().hashCode();
	}


	Object around(RelationshipBacked entity) : entityFieldGet(entity) {	
		FieldSignature fieldSignature=(FieldSignature) thisJoinPoint.getSignature();
		Field f = fieldSignature.getField();

		if (isStartNodeField(f)) {
			Node startNode = entity.getUnderlyingRelationship().getStartNode();
			if (startNode == null) {
				return null;
			}
			return graphEntityInstantiator.createEntityFromState(startNode, (Class<? extends NodeBacked>) f.getType());
		}
		if (isEndNodeField(f)) {
			Node endNode = entity.getUnderlyingRelationship().getEndNode();
			if (endNode == null) {
				return null;
			}
			return graphEntityInstantiator.createEntityFromState(endNode, (Class<? extends NodeBacked>) f.getType());
		}
		
//		TODO fix arrays, TODO serialize other types as byte[] or string (for indexing, querying) via Annotation
		if (isPropertyType(f.getType())) {
			Relationship rel = entity.getUnderlyingRelationship();
			if (rel == null) {
				throw new InvalidDataAccessApiUsageException("Please set start node and end node before reading from other fields.");
			}
			String propName = FieldAccessorFactory.getNeo4jPropertyName(f);
			log.info("GET " + f + " <- Neo4J simple relationship property [" + propName + "]");
			return rel.getProperty(propName, null);
		}
		
		return proceed(entity);
	}
	
	Object around(RelationshipBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
		try {
			FieldSignature fieldSignature = (FieldSignature) thisJoinPoint.getSignature();
			Field f = fieldSignature.getField();
			if (isStartNodeField(f) || isEndNodeField(f)) {
				throw new InvalidDataAccessApiUsageException("Cannot change start node or end node of existing relationship.");
			}
			if (isPropertyType(f.getType())) {
				Relationship rel = entity.getUnderlyingRelationship();
				if (rel == null) {
					throw new InvalidDataAccessApiUsageException("Please set start node and end node before assigning to other fields.");
				}
				String propName = FieldAccessorFactory.getNeo4jPropertyName(f);
                if (newVal==null) {
                    entity.getUnderlyingRelationship().removeProperty(propName);
                }
                else {
                    entity.getUnderlyingRelationship().setProperty(propName, newVal);
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
	
	private boolean isPropertyType(Class<?> fieldType) {
		// todo: add array support
		return fieldType.isPrimitive()
	  		|| (fieldType.isArray() && !fieldType.getComponentType().isArray() && isPropertyType(fieldType.getComponentType()))
	  		|| fieldType.equals(String.class)
	  		|| fieldType.equals(Character.class)
	  		|| fieldType.equals(Boolean.class)
	  		|| (fieldType.getName().startsWith("java.lang") && Number.class.isAssignableFrom(fieldType));
	}

}
