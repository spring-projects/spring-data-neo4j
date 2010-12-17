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
 * Aspect for handling node entity creation and field access (read & write)
 * puts the underlying state (Node) into and delegates field access to an {@link EntityStateAccessors} instance,
 * created by a configured {@link NodeEntityStateAccessorsFactory}.
 *
 * Handles constructor invocation and partial entities as well.
 */
public aspect Neo4jNodeBacking extends AbstractTypeAnnotatingMixinFields<NodeEntity, NodeBacked> {
    private GraphDatabaseContext graphDatabaseContext;
    private NodeEntityStateAccessorsFactory entityStateAccessorsFactory;

    public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }
    public void setNodeEntityStateAccessorsFactory(NodeEntityStateAccessorsFactory entityStateAccessorsFactory) {
        this.entityStateAccessorsFactory = entityStateAccessorsFactory;
    }
    /**
     * pointcut for constructors not taking a node to be handled by the aspect and the {@link EntityStateAccessors}
     */
	pointcut arbitraryUserConstructorOfNodeBackedObject(NodeBacked entity) :
		execution((@NodeEntity *).new(..)) &&
		!execution((@NodeEntity *).new(Node)) &&
		this(entity) && !cflowbelow(call(* fromStateInternal(..)));


    /**
     * Handle outside entity instantiation by either creating an appropriate backing node in the graph or in the case
     * of a reinstantiated partial entity by assigning the original node to the entity, the concrete behaviour is delegated
     * to the {@link EntityStateAccessors}. Also handles the java type representation in the graph.
     * When running outside of a transaction, no node is created, this is handled later when the entity is accessed within
     * a transaction again.
     */
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

    /**
     * Backing state
     * @deprecated
     */
	private transient Node NodeBacked.underlyingNode;
    /**
     * State accessors that encapsulate the underlying state and the behaviour related to it (field access, creation)
     */
    private transient EntityStateAccessors<NodeBacked,Node> NodeBacked.stateAccessors;

    /**
     * sets the underlying state to the given node, creates an {@link EntityStateAccessors} instance on demand for delegating
     * the behaviour, otherwise just updates the backing state
     * @param n the node to be the backing state of the entity
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

    /**
     * creates a relationship to the target node entity with the given relationship type
     * @param target entity
     * @param type neo4j relationship type for the underlying relationship
     * @return the newly created relationship to the target node
     */
	public Relationship NodeBacked.relateTo(NodeBacked target, RelationshipType type) {
		return this.underlyingNode.createRelationshipTo(target.getUnderlyingState(), type);
	}

    /**
     * @return node id or null if there is no underlying state
     */
	public Long NodeBacked.getNodeId() {
        if (!hasUnderlyingNode()) return null;
		return underlyingNode.getId();
	}

    /**
     * handles traversal from the current node with the given traversal description, the entities returned must be instances of the
     * provided target type
     * @param targetType node entity java types of the traversal results
     * @param traversalDescription
     * @return lazy Iterable over the traversal results, converted to the expected node entity instances
     */
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

    /**
     * Creates a relationship to the target node  with the given relationship type.
     * @param target node
     * @param relationshipClass  expected relationship class of the resulting relationship entity
     * @param relationshipType
     * @return relationship entity, instance of the provided relationshipClass
     */
    public RelationshipBacked NodeBacked.relateTo(NodeBacked target, Class<? extends RelationshipBacked> relationshipClass, String relationshipType) {
        Relationship rel = this.getUnderlyingState().createRelationshipTo(target.getUnderlyingState(), DynamicRelationshipType.withName(relationshipType));
        return Neo4jNodeBacking.aspectOf().graphDatabaseContext.createEntityFromState(rel, relationshipClass);
    }

    /**
     * removes the relationship to the target node entity with the given relationship type
     * @param target node entity
     * @param relationshipType
     */
    public void NodeBacked.removeRelationshipTo(NodeBacked target, String relationshipType) {
        Node myNode=this.getUnderlyingState();
        Node otherNode=target.getUnderlyingState();
        for (Relationship rel : this.getUnderlyingState().getRelationships(DynamicRelationshipType.withName(relationshipType))) {
            if (rel.getOtherNode(myNode).equals(otherNode)) {
                rel.delete();
                return;
            }
        }
    }

    /**
     * introduced method for accessing and Relationship Entity instance for the given start node and relationship type.
     * @param node start node
     * @param relationshipClass class of the relationship entity
     * @param type type of the graph relationship
     * @return and instance of the requested relationshipClass if the relationship was found, null otherwise
     */
    public RelationshipBacked NodeBacked.getRelationshipTo(NodeBacked node, Class<? extends RelationshipBacked> relationshipClass, String type) {
        Node myNode=this.getUnderlyingState();
        Node otherNode=node.getUnderlyingState();
        for (Relationship rel : this.getUnderlyingState().getRelationships(DynamicRelationshipType.withName(type))) {
            if (rel.getOtherNode(myNode).equals(otherNode)) return Neo4jNodeBacking.aspectOf().graphDatabaseContext.createEntityFromState(rel, relationshipClass);
        }
        return null;
    }

    /**
     * @param obj
     * @return result of equals operation fo the underlying node, false if there is none
     */
	public final boolean NodeBacked.equals(Object obj) {
        if (obj == this) return true;
        if (!hasUnderlyingNode()) return false;
		if (obj instanceof NodeBacked) {
			return this.getUnderlyingState().equals(((NodeBacked) obj).getUnderlyingState());
		}
		return false;
	}

    /**
     * @return result of the hashCode of the underlying node (if any, otherwise identityHashCode)
     */
	public final int NodeBacked.hashCode() {
        if (!hasUnderlyingNode()) return System.identityHashCode(this);
		return getUnderlyingState().hashCode();
	}

    /**
     * delegates field reads to the state accessors instance
     */
    Object around(NodeBacked entity): entityFieldGet(entity) {
        Object result=entity.stateAccessors.getValue(field(thisJoinPoint));
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    /**
     * delegates field writes to the state accessors instance
     */
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
