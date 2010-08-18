package org.springframework.persistence.graph.neo4j;

import java.lang.reflect.Field;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.util.GraphDatabaseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.persistence.graph.Graph;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;
import org.springframework.persistence.support.EntityInstantiator;

/**
 * Aspect to turn an object annotated with GraphEntity into a graph entity using Neo4J.
 * Delegates all field access (except for fields assumed to be transient)
 * to an underlying Neo4 graph node.
 * 
 * @author Rod Johnson
 */
public aspect Neo4jRelationshipBacking extends AbstractTypeAnnotatingMixinFields<Graph.Relationship,RelationshipBacked> {
	
	//-------------------------------------------------------------------------
	// Configure aspect for whole system.
	// init() method can be invoked automatically if the aspect is a Spring
	// bean, or called in user code.
	//-------------------------------------------------------------------------
	// Aspect shared Neo4J Graph Database Service
	private EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
	
	@Autowired
	public void init(EntityInstantiator<NodeBacked, Node> gei) {
		this.graphEntityInstantiator = gei;
	}
	
	// Introduced fields
	private Relationship RelationshipBacked.underlyingRelationship;
	private Node RelationshipBacked.underlyingStartNode;
	private Node RelationshipBacked.underlyingEndNode;
	
	public void RelationshipBacked.setUnderlyingRelationship(Relationship r) {
		this.underlyingRelationship = r;
		underlyingStartNode = r.getStartNode();
		underlyingEndNode = r.getEndNode();
	}
	
	public Relationship RelationshipBacked.getUnderlyingRelationship() {
		return underlyingRelationship;
	}
	
	public void RelationshipBacked.setUnderlyingStartNode(Node n) {
		underlyingStartNode = n;
	}
	
	public Node RelationshipBacked.getUnderlyingStartNode() {
		return underlyingStartNode;
	}
	
	public void RelationshipBacked.setUnderlyingEndNode(Node n) {
		underlyingEndNode = n;
	}
	
	public Node RelationshipBacked.getUnderlyingEndNode() {
		return underlyingEndNode;
	}

	public long RelationshipBacked.getId() {
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
			Node startNode = entity.getUnderlyingStartNode();
			if (startNode == null) {
				return null;
			}
			return graphEntityInstantiator.createEntityFromState(startNode, (Class<? extends NodeBacked>) f.getType());
		}
		if (isEndNodeField(f)) {
			Node endNode = entity.getUnderlyingEndNode();
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
			String propName = getNeo4jPropertyName(f);
			log.info("GET " + f + " <- Neo4J simple relationship property [" + propName + "]");
			return rel.getProperty(propName, null);
		}
		
		return proceed(entity);
	}
	
	Object around(RelationshipBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
		try {
			FieldSignature fieldSignature = (FieldSignature) thisJoinPoint.getSignature();
			Field f = fieldSignature.getField();
			if (newVal instanceof NodeBacked 
					&& isStartNodeField(f)
					&& entity.getUnderlyingStartNode() == null) {
				NodeBacked newValNb = (NodeBacked) newVal;
				entity.setUnderlyingStartNode(newValNb.getUnderlyingNode());
				if (entity.getUnderlyingEndNode() != null) {
					entity.setUnderlyingRelationship(entity.getUnderlyingStartNode().createRelationshipTo(entity.getUnderlyingEndNode(), DynamicRelationshipType.withName(f.getDeclaringClass().getSimpleName())));
				}
			}
			if (newVal instanceof NodeBacked 
					&& isEndNodeField(f)
					&& entity.getUnderlyingEndNode() == null) {
				NodeBacked newValNb = (NodeBacked) newVal;
				entity.setUnderlyingEndNode(newValNb.getUnderlyingNode());
				if (entity.getUnderlyingStartNode() != null) {
					entity.setUnderlyingRelationship(entity.getUnderlyingStartNode().createRelationshipTo(entity.getUnderlyingEndNode(), DynamicRelationshipType.withName(f.getDeclaringClass().getSimpleName())));
				}
			}
			if (isPropertyType(f.getType())) {
				Relationship rel = entity.getUnderlyingRelationship();
				if (rel == null) {
					throw new InvalidDataAccessApiUsageException("Please set start node and end node before assigning to other fields.");
				}
				String propName = getNeo4jPropertyName(f);
				entity.getUnderlyingRelationship().setProperty(propName, newVal);
				log.info("SET " + f + " -> Neo4J simple relationship property [" + propName + "] with value=[" + newVal + "]");
				return proceed(entity, newVal);
			}
			return proceed(entity, newVal);
		} catch(NotInTransactionException e) {
			throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
		}
	}

	private boolean isEndNodeField(Field f) {
		return f.isAnnotationPresent(Graph.Relationship.EndNode.class);
	}

	private boolean isStartNodeField(Field f) {
		return f.isAnnotationPresent(Graph.Relationship.StartNode.class);
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

	private static String getNeo4jPropertyName(Field field) {
		return String.format("%s.%s",field.getDeclaringClass().getSimpleName(),field.getName());
	}
	

}
