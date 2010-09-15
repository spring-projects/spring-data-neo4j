package org.springframework.datastore.graph.neo4j.spi.node;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.GraphEntityProperty;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;

import org.springframework.datastore.graph.api.GraphEntity;
import org.springframework.datastore.graph.neo4j.fieldaccess.*;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;
import org.springframework.util.ObjectUtils;

/**
 * Aspect to turn an object annotated with GraphEntity into a graph entity using Neo4J.
 * Delegates all field access (except for fields assumed to be transient)
 * to an underlying Neo4 graph node.
 * 
 * @author Rod Johnson
 */
public aspect Neo4jNodeBacking extends AbstractTypeAnnotatingMixinFields<GraphEntity, NodeBacked> {
    private GraphDatabaseContext graphDatabaseContext;
    private DelegatingFieldAccessorFactory fieldAccessorFactory;

    //-------------------------------------------------------------------------
	// Configure aspect for whole system.
	// init() method can be invoked automatically if the aspect is a Spring
	// bean, or called in user code.
	//-------------------------------------------------------------------------

    @Autowired
	public void init(GraphDatabaseContext ctx) {
        this.graphDatabaseContext = ctx;
        this.fieldAccessorFactory = new DelegatingFieldAccessorFactory(ctx);
	}
	
	
	//-------------------------------------------------------------------------
	// Advise user-defined constructors of NodeBacked objects to create a new
	// Neo4J backing node
	//-------------------------------------------------------------------------
	pointcut arbitraryUserConstructorOfNodeBackedObject(NodeBacked entity) : 
		execution((@GraphEntity *).new(..)) &&
		!execution((@GraphEntity *).new(Node)) &&
		this(entity);  // && !cflow(execution(* fromStateInternal(..));
	
	
	// Create a new node in the Graph if no Node was passed in a constructor
	before(NodeBacked entity) : arbitraryUserConstructorOfNodeBackedObject(entity) {
        entity.underlyingState=new DetachableEntityStateAccessors(new DefaultEntityStateAccessors<NodeBacked,Node>(null,entity,entity.getClass(),graphDatabaseContext));
        if (!graphDatabaseContext.transactionIsRunning()) {
            log.warn("New Nodebacked created outside of transaction "+ entity.getClass());

        } else {
            createAndAssignNode(entity);
        }
	}

    private void createAndAssignNode(NodeBacked entity) {
		try {
            Node node=graphDatabaseContext.createNode();
            entity.underlyingState.setNode(node);
			entity.setUnderlyingNode(node);
			log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getUnderlyingNode() +"]; " +
					"Updating metamodel");
			graphDatabaseContext.postEntityCreation(entity);
		} catch(NotInTransactionException e) {
			throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
		}
    }

    // Introduced field
	private Node NodeBacked.underlyingNode;
    private EntityStateAccessors<NodeBacked> NodeBacked.underlyingState;

    private Map<Field,Object> NodeBacked.dirty;

	public void NodeBacked.setUnderlyingNode(Node n) {
		this.underlyingNode = n;
	}
	
	public Node NodeBacked.getUnderlyingNode() {
		return underlyingNode;
	}
	
    public boolean NodeBacked.hasUnderlyingNode() {
        return underlyingNode!=null;
    }

	public Relationship NodeBacked.relateTo(NodeBacked nb, RelationshipType type) {
		return this.underlyingNode.createRelationshipTo(nb.getUnderlyingNode(), type);
	}

	public Long NodeBacked.getNodeId() {
        if (!hasUnderlyingNode()) return null;
		return underlyingNode.getId();
	}


    private boolean NodeBacked.isDirty() {
        return this.dirty!=null && !this.dirty.isEmpty();
    }
    private boolean NodeBacked.isDirty(Field f) {
        return this.dirty!=null && this.dirty.containsKey(f);
    }

    private void NodeBacked.clearDirty() {
        if (this.dirty!=null) this.dirty.clear();
    }
    private String NodeBacked.dump() {
        if (this.dirty!=null) return this.dirty.toString();
        return "";
    }

    private void NodeBacked.addDirty(Field f, Object previousValue) {
        if (this.dirty==null) this.dirty=new HashMap<Field, Object>();
        this.dirty.put(f,previousValue);
    }

    public  Iterable<? extends NodeBacked> NodeBacked.find(final Class<? extends NodeBacked> targetType, TraversalDescription traversalDescription) {
        if (!hasUnderlyingNode()) throw new IllegalStateException("No node attached to " + this);
        final Traverser traverser = traversalDescription.traverse(this.getUnderlyingNode());
        return new NodeBackedNodeIterableWrapper(traverser, targetType, Neo4jNodeBacking.aspectOf().graphDatabaseContext);
    }
    /*
    public Iterable<? extends NodeBacked> NodeBacked.traverse(TraversalDescription traversalDescription) {
        final Class<? extends NodeBacked> target = this.getClass();
        return this.traverse(target,traversalDescription);
    }
    */

    /*
    public <R extends RelationshipBacked, N extends NodeBacked> R NodeBacked.relateTo(N node, Class<R> relationshipType, String type) {
        Relationship rel = this.getUnderlyingNode().createRelationshipTo(node.getUnderlyingNode(), DynamicRelationshipType.withName(type));
        Neo4jNodeBacking.aspectOf().relationshipEntityInstantiator.createEntityFromState(rel, relationshipType);
    }
    */
    public RelationshipBacked NodeBacked.relateTo(NodeBacked node, Class<? extends RelationshipBacked> relationshipType, String type) {
        Relationship rel = this.getUnderlyingNode().createRelationshipTo(node.getUnderlyingNode(), DynamicRelationshipType.withName(type));
        return Neo4jNodeBacking.aspectOf().graphDatabaseContext.createEntityFromState(rel, relationshipType);
    }

    public RelationshipBacked NodeBacked.getRelationshipTo(NodeBacked node, Class<? extends RelationshipBacked> relationshipType, String type) {
        Node myNode=this.getUnderlyingNode();
        Node otherNode=node.getUnderlyingNode();
        for (Relationship rel : this.getUnderlyingNode().getRelationships(DynamicRelationshipType.withName(type))) {
            if (rel.getOtherNode(myNode).equals(otherNode)) return Neo4jNodeBacking.aspectOf().graphDatabaseContext.createEntityFromState(rel, relationshipType);
        }
        return null;
    }

    private Iterable<Map.Entry<Field,Object>> NodeBacked.eachDirty() {
        return this.dirty!=null ? this.dirty.entrySet() : Collections.<Field, Object>emptyMap().entrySet();
    }

	//-------------------------------------------------------------------------
	// Equals and hashCode for Neo4j entities.
	// Final to prevent overriding.
	//-------------------------------------------------------------------------
	// TODO could use template method for further checks if needed
	public final boolean NodeBacked.equals(Object obj) {
        if (obj == this) return true;
        if (!hasUnderlyingNode()) return false;
		if (obj instanceof NodeBacked) {
			return this.getUnderlyingNode().equals(((NodeBacked) obj).getUnderlyingNode());
		}
		return false;
	}
	
	public final int NodeBacked.hashCode() {
        if (!hasUnderlyingNode()) return System.identityHashCode(this);
		return getUnderlyingNode().hashCode();
	}

    private Object getValueFromEntity(Field field, NodeBacked entity) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing field "+field+" in "+entity.getClass(),e);
        }
    }
    /*
    always runs inside a transaction
     */
    private ShouldProceedOrReturn getNodePropertyOrRelationship(Field field, NodeBacked entity) {
        // TODO fix arrays, TODO serialize other types as byte[] or string (for indexing, querying) via Annotation
        if (isIdField(field)) return new ShouldProceedOrReturn(entity.getUnderlyingNode().getId());
        if (Modifier.isTransient(field.getModifiers())) return new ShouldProceedOrReturn();
        if (isNeo4jPropertyType(field.getType()) || isDeserializableField(field)) {
            String propName = DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
            log.info("GET " + field + " <- Neo4J simple node property [" + propName + "]");
            Node node = entity.getUnderlyingNode();
            Object nodeProperty = deserializePropertyValue(node.getProperty(propName, getDefaultValue(field.getType())), field.getType());
            return new ShouldProceedOrReturn(nodeProperty);
        }

        FieldAccessor accessor = fieldAccessorFactory.forField(field);
        if (accessor!=null) {
            Object obj = accessor.getValue(entity);
            if (obj != null) {
                return new ShouldProceedOrReturn(obj);
            }
        }
        log.info("Ignored GET " + field + ": " + field.getType().getName() + " not primitive or GraphEntity");
        return new ShouldProceedOrReturn();
    }

    private boolean isIdField(Field field) {
        if (!field.getName().equals("id")) return false;
        final Class<?> type = field.getType();
        return type.equals(Long.class) || type.equals(long.class);
    }

    private Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type.equals(boolean.class)) return false;
            return 0;
        }
        return null;
    }

    private void flushDirty(NodeBacked entity) {
        if (graphDatabaseContext.transactionIsRunning()) {
            final boolean newNode=!entity.hasUnderlyingNode();
            if (newNode) {
                createAndAssignNode(entity);
            }
            if (entity.isDirty()) {
                for (final Map.Entry<Field,Object> entry : entity.eachDirty()) {
                    final Field field = entry.getKey();
                    log.warn("Flushing dirty Entity new node "+newNode+" field "+field);
                    if (!newNode) {
                        checkConcurrentModification(entity, entry, field);
                    }
                    setNodePropertyOrRelationship(field, entity, getValueFromEntity(field,entity));
                }
                entity.clearDirty();
            }
        }
    }



    private void checkConcurrentModification(NodeBacked entity, Map.Entry<Field, Object> entry, Field field) {
        final Object nodeValue = getNodePropertyOrRelationship(field, entity).value;
        final Object previousValue = entry.getValue();
        if (!ObjectUtils.nullSafeEquals(nodeValue,previousValue)) {
            throw new ConcurrentModificationException("Node "+entity.getUnderlyingNode()+" field "+field+" changed in between previous "+ previousValue +" current "+nodeValue); // todo or just overwrite
        }
    }

    /*
    always called inside a transaction
     */

    private ShouldProceedOrReturn setNodePropertyOrRelationship(Field field, NodeBacked entity, Object newVal) {
        try {
            if (isIdField(field)) return new ShouldProceedOrReturn(null);
            if (Modifier.isFinal(field.getModifiers())) return new ShouldProceedOrReturn(newVal);
            if (Modifier.isTransient(field.getModifiers())) return new ShouldProceedOrReturn(true, newVal);
            if (isNeo4jPropertyType(field.getType()) || isSerializableField(field)) {
                String propName = DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
                Node node = entity.getUnderlyingNode();
                if (newVal==null) {
                    node.removeProperty(propName);
                    if (isIndexedProperty(field)) graphDatabaseContext.removeIndex(node,propName);
                } else {
                    node.setProperty(propName, serializePropertyValue(newVal, field.getType()));
                    if (isIndexedProperty(field)) graphDatabaseContext.index(node,propName,newVal);
                }
                log.info("SET " + field + " -> Neo4J simple node property [" + propName + "] with value=[" + newVal + "]");
                return new ShouldProceedOrReturn(true,newVal);
            }

            FieldAccessor accessor = fieldAccessorFactory.forField(field);
            if (accessor == null) {
                log.info("Ignored SET " + field + ": " + field.getType().getName() + " not primitive, convertable, or GraphEntity");
                return new ShouldProceedOrReturn(true,newVal);
            }
            log.info("SET " + field + " -> Neo4J relationship with value=[" + newVal + "]");
            Object result = accessor.setValue(entity, newVal);
            return new ShouldProceedOrReturn(true,result);
        } catch(NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }
    Object around(NodeBacked entity): entityFieldGet(entity) {
        FieldSignature fieldSignature = (FieldSignature) thisJoinPoint.getSignature();
        Field f = fieldSignature.getField();

        if (!graphDatabaseContext.transactionIsRunning()) {
            log.warn("Outside of transaction, GET value from field "+f+" has node "+entity.hasUnderlyingNode()+" dirty "+entity.isDirty(f)+" "+entity.dump());
            if (!entity.hasUnderlyingNode() || (entity.isDirty(f))) {
                log.warn("Outside of transaction, GET value from field "+f);
                return proceed(entity);
            }
        }
        log.trace("Inside of transaction, GET-FLUSH to field "+f+" has node "+entity.hasUnderlyingNode()+" dirty "+entity.isDirty()+" "+entity.dump());

        flushDirty(entity);

        Object result=entity.underlyingState.getValue(f);
        if (result instanceof DoReturn) return ((DoReturn)result).value;
        return proceed(entity);
        /*
        ShouldProceedOrReturn shouldProceedOrReturn=getNodePropertyOrRelationship(f,entity);
        if (shouldProceedOrReturn.proceed) {
            return proceed(entity);
        } else {
            return shouldProceedOrReturn.value;
        }
        */
    }

    Object around(NodeBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        FieldSignature fieldSignature=(FieldSignature) thisJoinPoint.getSignature();
        Field f = fieldSignature.getField();
        if (!graphDatabaseContext.transactionIsRunning()) {
            log.warn("Outside of transaction, SET value "+newVal+" to field "+f+" has node "+entity.hasUnderlyingNode()+" dirty "+entity.isDirty(f));
            if (!entity.isDirty(f)) {
                Object existingValue;
                if (entity.hasUnderlyingNode()) existingValue = getNodePropertyOrRelationship(f, entity).value;
                else {
                    existingValue = getValueFromEntity(f, entity);
                    if (existingValue==null) existingValue=getDefaultValue(f.getType());
                }
                entity.addDirty(f,existingValue);
            }
            log.warn("Outside of transaction, SET2 value "+newVal+" to field "+f+" has node "+entity.hasUnderlyingNode()+" dirty "+entity.isDirty(f)+" "+entity.dump());
            return proceed(entity, newVal);
        }
        log.trace("Inside of transaction, FLUSH-SET value "+newVal+" to field "+f+" has node "+entity.hasUnderlyingNode()+" dirty "+entity.isDirty()+" "+entity.dump());
        flushDirty(entity);


        Object result=entity.underlyingState.setValue(f,newVal);
        if (result instanceof DoReturn) return ((DoReturn)result).value;
        return proceed(entity,result);

        /*
        ShouldProceedOrReturn shouldProceedOrReturn=setNodePropertyOrRelationship(f,entity,newVal);
        if (shouldProceedOrReturn.proceed) {
            return proceed(entity,shouldProceedOrReturn.value);
        } else {
            return shouldProceedOrReturn.value;
        }
        */
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
		return !DelegatingFieldAccessorFactory.isRelationshipField(field) && graphDatabaseContext.canConvert(field.getType(), String.class);
	}
	
	private boolean isDeserializableField(Field field) {
		return !DelegatingFieldAccessorFactory.isRelationshipField(field) && graphDatabaseContext.canConvert(String.class, field.getType());
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
	
    // todo @property annotation
    // todo fieldlist in @graphentity
    private boolean isIndexedProperty(Field field) {
        final GraphEntity graphEntity = field.getDeclaringClass().getAnnotation(GraphEntity.class);
        if (graphEntity != null && graphEntity.fullIndex()) return true;
        final GraphEntityProperty graphEntityProperty = field.getAnnotation(GraphEntityProperty.class);
        return graphEntityProperty!=null && graphEntityProperty.index();
    }

}
