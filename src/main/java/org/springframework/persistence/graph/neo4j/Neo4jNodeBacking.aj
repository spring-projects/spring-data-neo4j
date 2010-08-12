package org.springframework.persistence.graph.neo4j;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.util.GraphDatabaseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.persistence.graph.GraphEntity;
import org.springframework.persistence.graph.Relationship;
import org.springframework.persistence.graph.neo4j.Neo4jNodeBacking.RelationshipInfoFactory;
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

	private RelationshipInfoFactory relationshipInfoFactory;
	
	@Autowired
	public void init(GraphDatabaseService gds, EntityInstantiator<NodeBacked, Node> gei) {
		this.graphDatabaseService = gds;
		this.graphEntityInstantiator = gei;
		this.graphDatabaseUtil = new GraphDatabaseUtil(gds);
		this.relationshipInfoFactory = new RelationshipInfoFactory(graphEntityInstantiator);
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
		try {
			entity.setUnderlyingNode(graphDatabaseService.createNode());
			log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getUnderlyingNode() +"]; " +
					"Updating metamodel");
			// TODO pull naming out into a strategy interface, todo a separate one, or the Entity Instatiator
			// graphEntityInstantiator.postEntityCreation(entity);
			postEntityCreation(entity);
		} catch(NotInTransactionException e) {
			throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
		}
	}

	private void postEntityCreation(NodeBacked entity) {
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
		
		RelationshipInfo relInfo = relationshipInfoFactory.forField(f);
		Object obj = relInfo.readObject(entity);
		if (obj == null) {
			log.info("Ignored GET " + f + ": " + f.getType().getName() + " not primitive or GraphEntity");
			return proceed(entity);
		}
		return obj;
	}
	
	Object around(NodeBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
		try {
			FieldSignature fieldSignature=(FieldSignature) thisJoinPoint.getSignature();
			Field f = fieldSignature.getField();
			// TODO fix arrays
			if (f.getType().isPrimitive() || f.getType().equals(String.class)) {
				String propName = getNeo4jPropertyName(f);
				entity.getUnderlyingNode().setProperty(propName, newVal);
				log.info("SET " + f + " -> Neo4J simple node property [" + propName + "] with value=[" + newVal + "]");
				return null;
			}
			
			RelationshipInfo relInfo = relationshipInfoFactory.forField(f);
			if (relInfo == null) {
				log.info("Ignored SET " + f + ": " + f.getType().getName() + " not primitive or GraphEntity");
				return proceed(entity, newVal);
			}
			log.info("SET " + f + " -> Neo4J relationship with value=[" + newVal + "]");
			relInfo.apply(entity, newVal);
			return null;
		} catch(NotInTransactionException e) {
			throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
		}
	}
	
	private boolean isSingleRelationshipField(Field f) {
		return f.getType().isAnnotationPresent(GraphEntity.class);
		//return NodeBacked.class.isAssignableFrom(f.getType());
	}
	
	private static String getNeo4jPropertyName(Field field) {
		return String.format("%s.%s",field.getDeclaringClass().getSimpleName(),field.getName());
	}
	
	public static interface RelationshipInfo {
		// Set entity field to newVal
		void apply(NodeBacked entity, Object newVal);

		// Read object from entity field
		Object readObject(NodeBacked entity);
	}
	
	public static class RelationshipInfoFactory {
		private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

		public RelationshipInfoFactory(EntityInstantiator<NodeBacked,Node> graphEntityInstantiator) {
			this.graphEntityInstantiator = graphEntityInstantiator;
		}
		
		public RelationshipInfo forField(Field field) {
			if (isSingleRelationshipField(field)) {
				final Relationship relAnnotation = field.getAnnotation(Relationship.class);
				if (relAnnotation != null) {
					return new SingleRelationshipInfo(DynamicRelationshipType.withName(relAnnotation.type()), 
							relAnnotation.direction().toNeo4jDir(), field.getType(), graphEntityInstantiator);
				}
				return new SingleRelationshipInfo(DynamicRelationshipType.withName(getNeo4jPropertyName(field)), 
						Direction.OUTGOING, field.getType(), graphEntityInstantiator);
			}
			if (isOneToNRelationshipField(field)) {
				final Relationship relAnnotation = field.getAnnotation(Relationship.class);
				return new OneToNRelationshipInfo(DynamicRelationshipType.withName(relAnnotation.type()), 
						relAnnotation.direction().toNeo4jDir(), relAnnotation.elementClass(), graphEntityInstantiator);
			}
			throw new IllegalArgumentException("Not a Neo4j relationship field.");
		}

		private static boolean isSingleRelationshipField(Field f) {
//			return f.getType().isAnnotationPresent(GraphEntity.class);
			return NodeBacked.class.isAssignableFrom(f.getType());
		}
		
		private static boolean isOneToNRelationshipField(Field f) {
			return f.isAnnotationPresent(Relationship.class) 
				&& !Void.class.isAssignableFrom(f.getAnnotation(Relationship.class).elementClass())
				&& Collection.class.isAssignableFrom(f.getType());
		}
	}
	
	public static class SingleRelationshipInfo implements RelationshipInfo {

		private final RelationshipType type;
		private final Direction direction;
		private final Class<?> clazz;
		private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
		
		public SingleRelationshipInfo(RelationshipType type, Direction direction, Class<?> clazz, EntityInstantiator<NodeBacked,Node> graphEntityInstantiator) {
			this.type = type;
			this.direction = direction;
			this.clazz = clazz;
			this.graphEntityInstantiator = graphEntityInstantiator;
		}

		public void apply(NodeBacked entity, Object newVal) {
			if (newVal != null && !(newVal instanceof NodeBacked)) {
				throw new IllegalArgumentException("New value must be NodeBacked.");
			}
			Node entityNode = entity.getUnderlyingNode();
			for ( org.neo4j.graphdb.Relationship relationship : entityNode.getRelationships(type, direction) ) {
				relationship.delete();
			}
			if (newVal == null) {
				return;
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
		}

		@Override
		public Object readObject(NodeBacked entity) {
			// does it have to be there, isn't it enough to have a nodebacked as target?
			Node entityNode = entity.getUnderlyingNode();
			if (entityNode == null) {
				throw new IllegalStateException("Entity must have a backing Node");
			}
			org.neo4j.graphdb.Relationship singleRelationship = entityNode.getSingleRelationship(type, direction);
			
			// TODO is this correct
			// [mh] i assume only for null
			if (singleRelationship == null) {
				return null;
			}
			Node targetNode = singleRelationship.getOtherNode(entityNode);
//			log.info("GET " + f + ": " + f.getType().getName() + ": setting from relationship " + singleRelationship);
			return graphEntityInstantiator.createEntityFromState(targetNode, (Class<NodeBacked>) clazz);
		}
		
	}
	
	public static class OneToNRelationshipInfo implements RelationshipInfo {

		private final RelationshipType type;
		private final Direction direction;
		private final Class<?> elementClass;
		private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

		public OneToNRelationshipInfo(RelationshipType type, Direction direction, Class<?> elementClass, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
			this.type = type;
			this.direction = direction;
			this.elementClass = elementClass;
			this.graphEntityInstantiator = graphEntityInstantiator;
		}

		public void apply(NodeBacked entity, Object newVal) {
			Node entityNode = entity.getUnderlyingNode();
			
			if (newVal != null) {
				if (!(newVal instanceof Set)) {
					throw new IllegalArgumentException("New value must be a Set, was: " + newVal.getClass());
				}
				Set<Object> set = (Set<Object>) newVal;
				for (Object obj : set) {
					if (!(obj instanceof NodeBacked)) {
						throw new IllegalArgumentException("New value elements must be NodeBacked.");
					}
					if (entityNode.equals(((NodeBacked) obj).getUnderlyingNode())) {
						throw new InvalidDataAccessApiUsageException("Cannot create circular reference.");
					}
				}
			}
			for ( org.neo4j.graphdb.Relationship relationship : entityNode.getRelationships(type, direction) ) {
				relationship.delete();
			}
			if (newVal == null) {
				return;
			}
			
			for (Object obj : (Set<Object>) newVal) {
				NodeBacked nb = (NodeBacked) obj;
				Node targetNode = nb.getUnderlyingNode();
				switch(direction) {
					case OUTGOING : entityNode.createRelationshipTo(targetNode, type); break;
					case INCOMING : targetNode.createRelationshipTo(entityNode, type); break;
					default : throw new IllegalArgumentException("invalid direction " + direction); 
				}
			}
			
		}
		
		@Override
		public Object readObject(NodeBacked entity) {
			Node entityNode = entity.getUnderlyingNode();
			if (entityNode == null) {
				throw new IllegalStateException("Entity must have a backing Node");
			}
			Iterable<org.neo4j.graphdb.Relationship> rels = entityNode.getRelationships(type, direction);
			Set<Object> result = new HashSet<Object>();
			for (org.neo4j.graphdb.Relationship rel : rels) {
				result.add(graphEntityInstantiator.createEntityFromState(rel.getOtherNode(entityNode), (Class<NodeBacked>) elementClass));
			}
			return result; 
		}
		
	}

}
