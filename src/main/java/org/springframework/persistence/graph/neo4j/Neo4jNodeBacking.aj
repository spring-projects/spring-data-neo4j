package org.springframework.persistence.graph.neo4j;

import java.lang.reflect.Field;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.util.GraphDatabaseUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
		// TODO pull naming out into a strategy interface, todo a separate one, or the Entity Instatiator
		// graphEntityInstantiator.postEntityCreation(entity);
		postEntityCreation(entity);
	}

	public void postEntityCreation(NodeBacked entity) {
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
		FieldSignature fieldSignature=(FieldSignature) thisJoinPoint.getSignature();
		Field f = fieldSignature.getField();
		
		// TODO fix arrays, TODO serialize other types as byte[] or string (for indexing, querying) via Annotation
		if (f.getType().isPrimitive() || f.getType().equals(String.class)) {
			String propName = getNeo4jPropertyName(f);
			log.info("GET " + f + " <- Neo4J simple node property [" + propName + "]");
			return entity.getUnderlyingNode().getProperty(propName, null);
		}
		
		// Look for a relationship
		if (isNeo4jRelationshipField(f)) {
			// does it have to be there, isn't it enough to have a nodebacked as target?
			Node me = entity.getUnderlyingNode();
			if (me == null) {
				throw new IllegalStateException("Entity must have a backing Node");
			}
			org.neo4j.graphdb.Relationship singleRelationship = getRelationship(me, f);
			
			// TODO is this correct
			// [mh] i assume only for null
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


	private org.neo4j.graphdb.Relationship getRelationship(Node me, Field field) {
		Relationship r = field.getAnnotation(Relationship.class);
		if (r == null) {
			RelationshipType type=DynamicRelationshipType.withName(getNeo4jPropertyName(field));
			return me.getSingleRelationship(type, Direction.OUTGOING);
		}
		RelationshipType type = DynamicRelationshipType.withName(r.type());
		org.neo4j.graphdb.Relationship singleRelationship = me.getSingleRelationship(type, r.direction().toNeo4jDir());
		return singleRelationship;
	}
	
	
	Object around(NodeBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
		FieldSignature fieldSignature=(FieldSignature) thisJoinPoint.getSignature();
		Field f = fieldSignature.getField();
		// TODO fix arrays
		if (f.getType().isPrimitive() || f.getType().equals(String.class)) {
			String propName = getNeo4jPropertyName(f);
			entity.getUnderlyingNode().setProperty(propName, newVal);
			log.info("SET " + f + " -> Neo4J simple node property [" + propName + "] with value=[" + newVal + "]");
			return null;
		}
		
		// Look for a relationship
		if (isNeo4jRelationshipField(f)) {

			org.neo4j.graphdb.Relationship relationship=getRelationship(entity.getUnderlyingNode(), f);
			if (relationship!=null) relationship.delete();
			addRelationship(entity, newVal, f);
			log.info("SET " + f + " -> Neo4J relationship with value=[" + newVal + "]");
			return null;
		}
		else { 
			log.info("Ignored SET " + f + ": " + f.getType().getName() + " not primitive or GraphEntity");
			return proceed(entity, newVal);
		}
	}


	private void addRelationship(NodeBacked entity, Object newVal, Field f) {
		Relationship r = f.getAnnotation(Relationship.class);
		if (r == null) {
			addRelationship(entity, (NodeBacked) newVal, getNeo4jPropertyName(f),Direction.OUTGOING);
		} else {
			addRelationship(entity, (NodeBacked) newVal, r.type(), r.direction().toNeo4jDir());
		}
	}
	
	// todo what happens to the previous value, remove relationships?
	private void addRelationship(NodeBacked entity, NodeBacked newVal, String relationshipName, Direction direction) {
		RelationshipType type = DynamicRelationshipType.withName(relationshipName);

		Node me = entity.getUnderlyingNode();
		Node targetNode = newVal.getUnderlyingNode();
		switch(direction) {
		case OUTGOING : me.createRelationshipTo(targetNode, type); break;
		case INCOMING : targetNode.createRelationshipTo(me, type); break;
		case BOTH : 
			me.createRelationshipTo(targetNode, type); 
			targetNode.createRelationshipTo(me, type);
			break;
		}
	}
	
	private boolean isNeo4jRelationshipField(Field f) {
		//return f.getType().isAnnotationPresent(GraphEntity.class);
		return NodeBacked.class.isAssignableFrom(f.getType());
	}
	
	
	private String getNeo4jPropertyName(Field field) {
		return String.format("%s.%s",field.getDeclaringClass().getSimpleName(),field.getName());
	}

}
