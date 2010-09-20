package org.springframework.datastore.graph.neo4j.spi.node;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.datastore.graph.api.GraphEntity;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.datastore.graph.neo4j.fieldaccess.*;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;

import java.lang.reflect.Field;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.unwrap;
import org.springframework.datastore.graph.neo4j.fieldaccess.DetachableEntityStateAccessorsFactory;

/**
 * Aspect to turn an object annotated with GraphEntity into a graph entity using Neo4J.
 * Delegates all field access (except for fields assumed to be transient)
 * to an underlying Neo4 graph node.
 * 
 * @author Rod Johnson
 */
public aspect Neo4jNodeBacking extends AbstractTypeAnnotatingMixinFields<GraphEntity, NodeBacked> {
    private GraphDatabaseContext graphDatabaseContext;
    private DetachableEntityStateAccessorsFactory entityStateAccessorsFactory;

    @Autowired
	public void init(GraphDatabaseContext ctx, DetachableEntityStateAccessorsFactory entityStateAccessorsFactory) {
        this.graphDatabaseContext = ctx;
        this.entityStateAccessorsFactory = entityStateAccessorsFactory;
	}

	//-------------------------------------------------------------------------
	// Advise user-defined constructors of NodeBacked objects to create a new Neo4J backing node
	//-------------------------------------------------------------------------
	pointcut arbitraryUserConstructorOfNodeBackedObject(NodeBacked entity) : 
		execution((@GraphEntity *).new(..)) &&
		!execution((@GraphEntity *).new(Node)) &&
		this(entity);  // && !cflow(execution(* fromStateInternal(..));
	
	
	// Create a new node in the Graph if no Node was passed in a constructor
	before(NodeBacked entity) : arbitraryUserConstructorOfNodeBackedObject(entity) {
        entity.underlyingState = entityStateAccessorsFactory.getEntityStateAccessors(entity);
        if (graphDatabaseContext.transactionIsRunning()) {
            entity.underlyingState.createAndAssignState();
        } else {
            log.warn("New Nodebacked created outside of transaction "+ entity.getClass());
        }
	}

    // Introduced field
	private Node NodeBacked.underlyingNode;
    private EntityStateAccessors<NodeBacked,Node> NodeBacked.underlyingState;

	public void NodeBacked.setUnderlyingState(Node n) {
		this.underlyingNode = n;
        if (this.underlyingState == null) {
            this.underlyingState = Neo4jNodeBacking.aspectOf().entityStateAccessorsFactory.getEntityStateAccessors(this);
        } else {
            this.underlyingState.setUnderlyingState(n);
        }
	}
	
	public Node NodeBacked.getUnderlyingState() {
		return underlyingNode;
	}
	
    public boolean NodeBacked.hasUnderlyingNode() {
        return underlyingNode!=null;
    }

	public Relationship NodeBacked.relateTo(NodeBacked nb, RelationshipType type) {
		return this.underlyingNode.createRelationshipTo(nb.getUnderlyingState(), type);
	}

	public Long NodeBacked.getNodeId() {
        if (!hasUnderlyingNode()) return null;
		return underlyingNode.getId();
	}

    public  Iterable<? extends NodeBacked> NodeBacked.find(final Class<? extends NodeBacked> targetType, TraversalDescription traversalDescription) {
        if (!hasUnderlyingNode()) throw new IllegalStateException("No node attached to " + this);
        final Traverser traverser = traversalDescription.traverse(this.getUnderlyingState());
        return new NodeBackedNodeIterableWrapper(traverser, targetType, Neo4jNodeBacking.aspectOf().graphDatabaseContext);
    }
    /* todo Andy Clement
    public Iterable<? extends NodeBacked> NodeBacked.traverse(TraversalDescription traversalDescription) {
        final Class<? extends NodeBacked> target = this.getClass();
        return this.traverse(target,traversalDescription);
    }
    */

    /* todo Andy Clement
    public <R extends RelationshipBacked, N extends NodeBacked> R NodeBacked.relateTo(N node, Class<R> relationshipType, String type) {
        Relationship rel = this.getUnderlyingState().createRelationshipTo(node.getUnderlyingState(), DynamicRelationshipType.withName(type));
        Neo4jNodeBacking.aspectOf().relationshipEntityInstantiator.createEntityFromState(rel, relationshipType);
    }
    */
    public RelationshipBacked NodeBacked.relateTo(NodeBacked node, Class<? extends RelationshipBacked> relationshipType, String type) {
        Relationship rel = this.getUnderlyingState().createRelationshipTo(node.getUnderlyingState(), DynamicRelationshipType.withName(type));
        return Neo4jNodeBacking.aspectOf().graphDatabaseContext.createEntityFromState(rel, relationshipType);
    }

    public RelationshipBacked NodeBacked.getRelationshipTo(NodeBacked node, Class<? extends RelationshipBacked> relationshipType, String type) {
        Node myNode=this.getUnderlyingState();
        Node otherNode=node.getUnderlyingState();
        for (Relationship rel : this.getUnderlyingState().getRelationships(DynamicRelationshipType.withName(type))) {
            if (rel.getOtherNode(myNode).equals(otherNode)) return Neo4jNodeBacking.aspectOf().graphDatabaseContext.createEntityFromState(rel, relationshipType);
        }
        return null;
    }

	public final boolean NodeBacked.equals(Object obj) {
        if (obj == this) return true;
        if (!hasUnderlyingNode()) return false;
		if (obj instanceof NodeBacked) {
			return this.getUnderlyingState().equals(((NodeBacked) obj).getUnderlyingState());
		}
		return false;
	}
	
	public final int NodeBacked.hashCode() {
        if (!hasUnderlyingNode()) return System.identityHashCode(this);
		return getUnderlyingState().hashCode();
	}

    Object around(NodeBacked entity): entityFieldGet(entity) {
        Object result=entity.underlyingState.getValue(field(thisJoinPoint));
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    Object around(NodeBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        Object result=entity.underlyingState.setValue(field(thisJoinPoint),newVal);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity,result);
	}

    Field field(JoinPoint joinPoint) {
        FieldSignature fieldSignature = (FieldSignature)joinPoint.getSignature();
        return fieldSignature.getField();
    }
}
