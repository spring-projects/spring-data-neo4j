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

package org.springframework.data.neo4j.aspects.support.node;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.annotation.*;

import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.aspects.core.RelationshipBacked;
import org.springframework.data.neo4j.mapping.RelationshipResult;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import org.springframework.data.neo4j.support.path.EntityPathPathIterableWrapper;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.neo4j.template.Neo4jOperations;

import javax.persistence.Transient;
import javax.persistence.Entity;

import java.lang.reflect.Field;
import java.util.Map;

import static org.springframework.data.neo4j.support.DoReturn.unwrap;

/**
 * Aspect for handling node entity creation and field access (read & write)
 * puts the underlying state (Node) into and delegates field access to an {@link org.springframework.data.neo4j.core.EntityState} instance,
 * created by a configured {@link NodeEntityStateFactory}.
 *
 * Handles constructor invocation and partial entities as well.
 */
public privileged aspect Neo4jNodeBacking { // extends AbstractTypeAnnotatingMixinFields<NodeEntity, NodeBacked> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    declare parents : (@NodeEntity *) implements NodeBacked;

    //declare @type: NodeBacked+: @Configurable;

    private Neo4jTemplate template;
    private NodeEntityStateFactory entityStateFactory;

    public void setTemplate(Neo4jTemplate template) {
        this.template = template;
    }
    public void setNodeEntityStateFactory(NodeEntityStateFactory entityStateFactory) {
        this.entityStateFactory = entityStateFactory;
    }

    declare @field: @GraphProperty * (@Entity @NodeEntity(partial=true) *).*:@Transient;
    declare @field: @RelatedTo * (@Entity @NodeEntity(partial=true) *).*:@Transient;
    declare @field: @RelatedToVia * (@Entity @NodeEntity(partial=true) *).*:@Transient;
    declare @field: @GraphId * (@Entity @NodeEntity(partial=true) *).*:@Transient;
    declare @field: @GraphTraversal * (@Entity @NodeEntity(partial=true) *).*:@Transient;
    declare @field: @Query * (@Entity @NodeEntity(partial=true) *).*:@Transient;



    protected pointcut entityFieldGet(NodeBacked entity) :
            get(!transient * NodeBacked+.*) &&
            this(entity) &&
            !get(* NodeBacked.*);


    protected pointcut entityFieldSet(NodeBacked entity, Object newVal) :
            set(!transient * NodeBacked+.*) &&
            this(entity) &&
            args(newVal) &&
            !set(* NodeBacked.*);


    /**
     * pointcut for constructors not taking a node to be handled by the aspect and the {@link org.springframework.data.neo4j.core.EntityState}
     */
	pointcut arbitraryUserConstructorOfNodeBackedObject(NodeBacked entity) :
		execution((@NodeEntity *).new(..)) &&
		!execution((@NodeEntity *).new(Node)) &&
		this(entity) && !cflowbelow(call(* fromStateInternal(..)));


    /**
     * Handle outside entity instantiation by either creating an appropriate backing node in the graph or in the case
     * of a reinstantiated partial entity by assigning the original node to the entity, the concrete behaviour is delegated
     * to the {@link org.springframework.data.neo4j.core.EntityState}. Also handles the java type representation in the graph.
     * When running outside of a transaction, no node is created, this is handled later when the entity is accessed within
     * a transaction again.
     */
    before(NodeBacked entity): arbitraryUserConstructorOfNodeBackedObject(entity) {
        if (entityStateFactory == null) {
            log.error("entityStateFactory not set, not creating accessors for " + entity.getClass());
        } else {
            if (entity.entityState != null) return;
            entity.entityState = entityStateFactory.getEntityState(entity, true, template);
        }
    }

    /**
     * State accessors that encapsulate the underlying state and the behaviour related to it (field access, creation)
     */
    private transient @Transient EntityState<Node> NodeBacked.entityState;

    public <T extends NodeBacked> T NodeBacked.persist() {
        return (T)this.entityState.persist();
    }

	public void NodeBacked.setPersistentState(Node n) {
        if (this.entityState == null) {
            this.entityState = Neo4jNodeBacking.aspectOf().entityStateFactory.getEntityState(this, false, getTemplate());
        }
        this.entityState.setPersistentState(n);
	}

	public Node NodeBacked.getPersistentState() {
		return this.entityState!=null ? this.entityState.getPersistentState() : null;
	}
	
    public EntityState<Node> NodeBacked.getEntityState() {
        return entityState;
    }

    public boolean NodeBacked.hasPersistentState() {
        return this.entityState!=null && this.entityState.hasPersistentState();
    }

    public <T extends NodeBacked> T NodeBacked.projectTo(Class<T> targetType) {
        return (T) template().projectTo( this, targetType);
    }

	public Relationship NodeBacked.relateTo(NodeBacked target, String type) {
        return this.relateTo(target, type, false);
    }
    public Relationship NodeBacked.relateTo(NodeBacked target, String type, boolean allowDuplicates) {
        final RelationshipResult result = entityStateHandler().createRelationshipBetween(this, target, type, allowDuplicates);
        return result.relationship;
	}

    public Relationship NodeBacked.getRelationshipTo(NodeBacked target, String type) {
        return template().getRelationshipBetween(this,target,type);
    }

    public Neo4jTemplate NodeBacked.getTemplate() {
        return Neo4jNodeBacking.aspectOf().template;
    }

	public Long NodeBacked.getNodeId() {
        if (!hasPersistentState()) return null;
		return getPersistentState().getId();
	}

    public  <T> Iterable<T> NodeBacked.findAllByTraversal(final Class<T> targetType, TraversalDescription traversalDescription) {
        if (!hasPersistentState()) throw new IllegalStateException("No node attached to " + this);
        return template().traverse(this.getPersistentState(), traversalDescription).to(targetType);
    }

    public  <T> Iterable<T> NodeBacked.findAllByQuery(final String query, final Class<T> targetType, Map<String,Object> params) {
        final CypherQueryExecutor executor = new CypherQueryExecutor(template().queryEngineFor());
        return executor.query(query, targetType,params);
    }

    public  Iterable<Map<String,Object>> NodeBacked.findAllByQuery(final String query,Map<String,Object> params) {
        final CypherQueryExecutor executor = new CypherQueryExecutor(template().queryEngineFor());
        return executor.queryForList(query,params);
    }

    public  <T> T NodeBacked.findByQuery(final String query, final Class<T> targetType,Map<String,Object> params) {
        final CypherQueryExecutor executor = new CypherQueryExecutor(template().queryEngineFor());
        return executor.queryForObject(query, targetType,params);
    }

    public <S extends NodeBacked, E extends NodeBacked> Iterable<EntityPath<S,E>> NodeBacked.findAllPathsByTraversal(TraversalDescription traversalDescription) {
        if (!hasPersistentState()) throw new IllegalStateException("No node attached to " + this);
        final Traverser traverser = traversalDescription.traverse(this.getPersistentState());
        return new EntityPathPathIterableWrapper<S, E>(traverser, template());
    }

    public <R extends RelationshipBacked, N extends NodeBacked> R NodeBacked.relateTo(N target, Class<R> relationshipClass, String relationshipType) {
        return template().createRelationshipBetween(this, target, relationshipClass, relationshipType, false);
    }
    public <R extends RelationshipBacked, N extends NodeBacked> R NodeBacked.relateTo(N target, Class<R> relationshipClass, String relationshipType, boolean allowDuplicates) {
        return template().createRelationshipBetween(this, target, relationshipClass, relationshipType, allowDuplicates);
    }

    public void NodeBacked.remove() {
        template().delete(this);
    }

    public void NodeBacked.removeRelationshipTo(NodeBacked target, String relationshipType) {
        template().deleteRelationshipBetween(this, target, relationshipType);
    }

    public <R extends RelationshipBacked> R NodeBacked.getRelationshipTo( NodeBacked target, Class<R> relationshipClass, String type) {
        return (R) template().getRelationshipBetween(this, target, relationshipClass, type);
    }

    public static Neo4jTemplate template() {
        return Neo4jNodeBacking.aspectOf().template;
    }

    /**
     * @param obj
     * @return result of equals operation fo the underlying node, false if there is none
     */
	public boolean NodeBacked.equals(Object obj) {
        final EntityStateHandler entityStateHandler = entityStateHandler();
        return entityStateHandler!=null ? entityStateHandler.equals(this, obj) : obj == this;
	}

    public static EntityStateHandler entityStateHandler() {
        final Neo4jTemplate template = template();
        return template!=null ? template.getEntityStateHandler() : null;
    }

    /**
     * @return result of the hashCode of the underlying node (if any, otherwise identityHashCode)
     */
	public int NodeBacked.hashCode() {
        final EntityStateHandler entityStateHandler = entityStateHandler();
        return entityStateHandler!=null ? entityStateHandler.hashCode(this) : System.identityHashCode(this);
	}

    /**
     * delegates field reads to the state accessors instance
     */
    Object around(NodeBacked entity): entityFieldGet(entity) {
        if (entity.entityState==null) return proceed(entity);
        Object result=entity.entityState.getValue(field(thisJoinPoint),null);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    /**
     * delegates field writes to the state accessors instance
     */
    Object around(NodeBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        if (entity.entityState==null) return proceed(entity,newVal);
        Object result=entity.entityState.setValue(field(thisJoinPoint),newVal,null);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity,result);
	}

    Field field(JoinPoint joinPoint) {
        FieldSignature fieldSignature = (FieldSignature)joinPoint.getSignature();
        return fieldSignature.getField();
    }

}
