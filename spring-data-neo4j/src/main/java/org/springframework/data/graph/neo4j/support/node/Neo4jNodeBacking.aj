/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.support.node;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.fieldaccess.*;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;

import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.unwrap;

import org.springframework.persistence.support.StateProvider;

/**
 * Aspect to turn an object annotated with GraphEntity into a graph entity using Neo4J.
 * Delegates all field access (except for fields assumed to be transient)
 * to an underlying Neo4 graph node.
 * 
 * @author Rod Johnson
 */
public aspect Neo4jNodeBacking extends AbstractTypeAnnotatingMixinFields<NodeEntity, NodeBacked> {
    private GraphDatabaseContext graphDatabaseContext;
    private NodeEntityStateAccessorsFactory entityStateAccessorsFactory;

    @Autowired
    public void init(GraphDatabaseContext graphDatabaseContext, NodeEntityStateAccessorsFactory entityStateAccessorsFactory) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.entityStateAccessorsFactory = entityStateAccessorsFactory;
    }
	//-------------------------------------------------------------------------
	// Advise user-defined constructors of NodeBacked objects to create a new Neo4J backing node
	//-------------------------------------------------------------------------
	pointcut arbitraryUserConstructorOfNodeBackedObject(NodeBacked entity) : 
		execution((@NodeEntity *).new(..)) &&
		!execution((@NodeEntity *).new(Node)) &&
		this(entity) && !cflowbelow(call(* fromStateInternal(..)));
	
	
	// Create a new node in the Graph if no Node was passed in a constructor
    before(NodeBacked entity): arbitraryUserConstructorOfNodeBackedObject(entity) {
        if (entityStateAccessorsFactory == null) {
            log.error("entityStateAccessorsFactory not set, not creating accessors for " + entity.getClass());
        } else {
            entity.stateAccessors = entityStateAccessorsFactory.getEntityStateAccessors(entity);
            Node node = StateProvider.retrieveState();
            if (node != null) {
                entity.setUnderlyingState(node);
            } else {
                if (graphDatabaseContext.transactionIsRunning()) {
                    entity.stateAccessors.createAndAssignState();
                } else {
                    log.warn("New Nodebacked created outside of transaction " + entity.getClass());
                }
            }

        }
    }

    // Introduced field
	private transient Node NodeBacked.underlyingNode;
    private transient EntityStateAccessors<NodeBacked,Node> NodeBacked.stateAccessors;

    /*
    public NodeBacked.new(Node n) {
       this.setUnderlyingState(n);
    }
    */
	public void NodeBacked.setUnderlyingState(Node n) {
		this.underlyingNode = n;
        if (this.stateAccessors == null) {
            this.stateAccessors = Neo4jNodeBacking.aspectOf().entityStateAccessorsFactory.getEntityStateAccessors(this);
        } else {
            this.stateAccessors.setUnderlyingState(n);
        }
	}
	
	public Node NodeBacked.getUnderlyingState() {
		return underlyingNode;
	}
	
    public EntityStateAccessors NodeBacked.getStateAccessors() {
        return stateAccessors;
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

    public void NodeBacked.removeRelationshipTo(NodeBacked node, String type) {
        Node myNode=this.getUnderlyingState();
        Node otherNode=node.getUnderlyingState();
        for (Relationship rel : this.getUnderlyingState().getRelationships(DynamicRelationshipType.withName(type))) {
            if (rel.getOtherNode(myNode).equals(otherNode)) {
                rel.delete();
                return;
            }
        }
        return;
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
        Object result=entity.stateAccessors.getValue(field(thisJoinPoint));
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    Object around(NodeBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        Object result=entity.stateAccessors.setValue(field(thisJoinPoint),newVal);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity,result);
	}

    Field field(JoinPoint joinPoint) {
        FieldSignature fieldSignature = (FieldSignature)joinPoint.getSignature();
        return fieldSignature.getField();
    }
}
