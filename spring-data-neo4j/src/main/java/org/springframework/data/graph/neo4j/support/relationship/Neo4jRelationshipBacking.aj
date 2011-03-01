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

package org.springframework.data.graph.neo4j.support.relationship;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.fieldaccess.*;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.annotation.*;

import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.unwrap;

/**
 * Aspect for handling relationship entity creation and field access (read & write)
 * puts the underlying state into and delegates field access to an {@link org.springframework.data.graph.neo4j.fieldaccess.EntityState} instance,
 * created by a configured {@link org.springframework.data.graph.neo4j.fieldaccess.RelationshipEntityStateFactory}
 */
public aspect Neo4jRelationshipBacking {
	
    protected final Log log = LogFactory.getLog(getClass());

    declare parents : (@RelationshipEntity *) implements RelationshipBacked;
    declare @type: RelationshipBacked+: @Configurable;


    protected pointcut entityFieldGet(RelationshipBacked entity) :
            get(* RelationshipBacked+.*) &&
            this(entity) &&
            !get(* RelationshipBacked.*);


    protected pointcut entityFieldSet(RelationshipBacked entity, Object newVal) :
            set(* RelationshipBacked+.*) &&
            this(entity) &&
            args(newVal) &&
            !set(* RelationshipBacked.*);

	private GraphDatabaseContext graphDatabaseContext;
    private RelationshipEntityStateFactory entityStateFactory;


    public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public void setRelationshipEntityStateFactory(RelationshipEntityStateFactory entityStateFactory) {
        this.entityStateFactory = entityStateFactory;
    }

    /**
     * field for {@link org.springframework.data.graph.neo4j.fieldaccess.EntityState} that takes care of all entity operations
     */
    private EntityState<RelationshipBacked,Relationship> RelationshipBacked.entityState;

    /**
     * creates a new {@link org.springframework.data.graph.neo4j.fieldaccess.EntityState} instance with the relationship parameter or updates an existing one
     * @param r
     */
	public void RelationshipBacked.setPersistentState(Relationship r) {
        if (this.entityState == null) {
            this.entityState = Neo4jRelationshipBacking.aspectOf().entityStateFactory.getEntityState(this);
        }
        this.entityState.setPersistentState(r);
	}
	
	public Relationship RelationshipBacked.getPersistentState() {
		return this.entityState.getPersistentState();
	}

	public boolean RelationshipBacked.hasUnderlyingRelationship() {
		return this.entityState.hasPersistentState();
	}

    /**
     * @return relationship id if there is an underlying relationship
     */
	public Long RelationshipBacked.getRelationshipId() {
        if (!hasUnderlyingRelationship()) return null;
		return getPersistentState().getId();
	}


    /**
     * @param obj
     * @return result of equality check of the underlying relationship
     */
	public final boolean RelationshipBacked.equals(Object obj) {
		if (obj instanceof RelationshipBacked) {
			return this.getPersistentState().equals(((RelationshipBacked) obj).getPersistentState());
		}
		return false;
	}

    /**
     * @return hashCode of the underlying relationship
     */
	public final int RelationshipBacked.hashCode() {
		return getPersistentState().hashCode();
	}

	public void RelationshipBacked.remove() {
	     Neo4jRelationshipBacking.aspectOf().graphDatabaseContext.removeRelationshipEntity(this);
	}

    public <R extends RelationshipBacked> R  RelationshipBacked.projectTo(Class<R> targetType) {
        return (R)Neo4jRelationshipBacking.aspectOf().graphDatabaseContext.projectTo(this, targetType);
    }

    Object around(RelationshipBacked entity): entityFieldGet(entity) {
        Object result=entity.entityState.getValue(field(thisJoinPoint));
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    Object around(RelationshipBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        Object result=entity.entityState.setValue(field(thisJoinPoint),newVal);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity,result);
	}


    Field field(JoinPoint joinPoint) {
        FieldSignature fieldSignature = (FieldSignature)joinPoint.getSignature();
        return fieldSignature.getField();
    }
}
