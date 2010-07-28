package org.springframework.persistence.graph.neo4j;

import java.lang.reflect.Field;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.util.GraphDatabaseUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.persistence.graph.Direction;
import org.springframework.persistence.graph.GraphEntity;
import org.springframework.persistence.graph.Relationship;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;
import org.springframework.persistence.support.EntityInstantiator;

/**
 * Aspect to turn an object annotated with GraphEntity into a graph entity using Neo4J.
 * Delegates all field access (except for fields assumed to be transient)
 * to an underlying Neo4 graph node.
 * 
 * @author Rod Johnson
 */
public aspect Neo4jNodeBacking extends AbstractTypeAnnotatingMixinFields<GraphEntity,NodeBacked> {
	
	//-------------------------------------------------------------------------
	// Configure aspect for whole system.
	// init() method can be invoked automatically if the aspect is a Spring
	// bean, or called in user code.
	//-------------------------------------------------------------------------
	// Aspect shared Neo4J Graph Database Service
	private GraphDatabaseService graphDatabaseService;
	
	private EntityInstantiator<NodeBacked,Node> graphEntityInstantiator;
	
	private GraphDatabaseUtil graphDatabaseUtil;
	
	@Autowired
	public void init(GraphDatabaseService gds, EntityInstantiator<NodeBacked, Node> gei) {
		this.graphDatabaseService = gds;
		this.graphEntityInstantiator = gei;
		this.graphDatabaseUtil = new GraphDatabaseUtil(gds);
	}
	
	
	//-------------------------------------------------------------------------
	// Advise user-defined constructors of NodeBacked objects to create a new
	// Neo4J backing node
	//-------------------------------------------------------------------------
	pointcut arbitraryUserConstructorOfNodeBackedObject(NodeBacked entity) : 
		execution((@GraphEntity *).new(..)) &&
		!execution((@GraphEntity *).new(Node)) &&
		this(entity);
	
	
	// Create a new node in the Graph if no Node was passed in a constructor
	before(NodeBacked entity) : arbitraryUserConstructorOfNodeBackedObject(entity) {
		entity.setUnderlyingNode(graphDatabaseService.createNode());
		log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getUnderlyingNode() +"]; " +
				"Updating metamodel");
		// TODO pull naming out into a strategy interface
		Node subReference = Neo4jHelper.findSubreferenceNode(entity.getClass(), graphDatabaseService);
		entity.getUnderlyingNode().createRelationshipTo(subReference, Neo4jHelper.INSTANCE_OF_RELATIONSHIP_TYPE);
		graphDatabaseUtil.incrementAndGetCounter(subReference, Neo4jHelper.SUBREFERENCE_NODE_COUNTER_KEY);
	}
	
	
	// Introduced field
	private Node NodeBacked.underlyingNode;
	
	public void NodeBacked.setUnderlyingNode(Node n) {
		this.underlyingNode = n;
	}
	
	public Node NodeBacked.getUnderlyingNode() {
		return underlyingNode;
	}

	
	//-------------------------------------------------------------------------
	// Equals and hashCode for Neo4j entities.
	// Final to prevent overriding.
	//-------------------------------------------------------------------------
	// TODO could use template method for further checks if needed
	public final boolean NodeBacked.equals(Object obj) {
		if (obj instanceof NodeBacked) {
			return this.getUnderlyingNode().equals(((NodeBacked) obj).getUnderlyingNode());
		}
		return false;
	}
	
	public final int NodeBacked.hashCode() {
		return getUnderlyingNode().hashCode();
	}


	Object around(NodeBacked entity) : entityFieldGet(entity) {	
		Field f = ((FieldSignature) thisJoinPoint.getSignature()).getField();
		
		// TODO fix arrays
		if (f.getType().isPrimitive() || f.getType().equals(String.class)) {
			String propName = getNeo4jPropertyName(thisJoinPoint.getSignature());
			log.info("GET " + f + " <- Neo4J simple node property [" + propName + "]");
			return entity.getUnderlyingNode().getProperty(propName, null);
		}
		
		// Look for a relationship
		if (isNeo4jRelationshipField(f)) {
			Relationship r = f.getAnnotation(Relationship.class);
			if (r == null) {
				throw new IllegalStateException("Must have @Relationship on " + f);
			}
			Node me = entity.getUnderlyingNode();
			if (me == null) {
				throw new IllegalStateException("Entity must have a backing Node");
			}
			RelationshipType type = DynamicRelationshipType.withName(r.type());
			org.neo4j.graphdb.Relationship singleRelationship = me.getSingleRelationship(type, r.direction().toNeo4jDir());
			
			// TODO is this correct
			if (singleRelationship == null) {
				log.info("GET " + f + ": " + f.getType().getName() + ": not set yet so returning field value");
				return proceed(entity);
			}
			Node targetNode = singleRelationship.getOtherNode(me);
			log.info("GET " + f + ": " + f.getType().getName() + ": setting from relationship " + singleRelationship);
			return graphEntityInstantiator.createEntityFromState(targetNode, (Class<NodeBacked>) f.getType());
		}
		
		// Must be transient
		log.info("Ignored GET " + f + ": " + f.getType().getName() + " not primitive or GraphEntity");
		return proceed(entity);
	}
	
	
	Object around(NodeBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
		Field f = ((FieldSignature) thisJoinPoint.getSignature()).getField();
		 
		// TODO fix arrays
		if (f.getType().isPrimitive() || f.getType().equals(String.class)) {
			String propName = getNeo4jPropertyName(thisJoinPoint.getSignature());
			entity.getUnderlyingNode().setProperty(propName, newVal);
			log.info("SET " + f + " -> Neo4J simple node property [" + propName + "] with value=[" + newVal + "]");
			return null;
		}
		
		// Look for a relationship
		if (isNeo4jRelationshipField(f)) {
			Relationship r = f.getAnnotation(Relationship.class);
			if (r == null) {
				throw new IllegalStateException("Must have @Relationship on " + f);
			}
			graphEntityFieldSet(entity, r, (NodeBacked) newVal);
			log.info("SET " + f + " -> Neo4J relationship with value=[" + newVal + "]");
			return null;
		}
		else { 
			log.info("Ignored SET " + f + ": " + f.getType().getName() + " not primitive or GraphEntity");
			return proceed(entity, newVal);
		}
	}
	
	
	private void graphEntityFieldSet(NodeBacked entity, Relationship r, NodeBacked newVal) {
		Node me = entity.getUnderlyingNode();
		RelationshipType type = DynamicRelationshipType.withName(r.type());
		Node targetNode = newVal.getUnderlyingNode();
		if (r.direction() == Direction.OUTGOING) {
			me.createRelationshipTo(targetNode, type);
		}
		else {
			targetNode.createRelationshipTo(me, type);
		}
	}
	
	private boolean isNeo4jRelationshipField(Field f) {
		//return f.getType().isAnnotationPresent(GraphEntity.class);
		return NodeBacked.class.isAssignableFrom(f.getType());
	}
	
	
	private String getNeo4jPropertyName(Signature sig) {
		return sig.toShortString();
	}

}
