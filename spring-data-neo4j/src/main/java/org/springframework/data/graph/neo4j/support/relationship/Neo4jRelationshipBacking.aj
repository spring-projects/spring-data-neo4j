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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.fieldaccess.*;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;

import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.unwrap;

public aspect Neo4jRelationshipBacking extends AbstractTypeAnnotatingMixinFields<RelationshipEntity,RelationshipBacked> {
	
	//-------------------------------------------------------------------------
	// Configure aspect for whole system.
	// init() method can be invoked automatically if the aspect is a Spring
	// bean, or called in user code.
	//-------------------------------------------------------------------------
	// Aspect shared Neo4J Graph Database Service
	private GraphDatabaseContext graphDatabaseContext;
    private RelationshipEntityStateAccessorsFactory entityStateAccessorsFactory;


    public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public void setRelationshipEntityStateAccessorsFactory(RelationshipEntityStateAccessorsFactory entityStateAccessorsFactory) {
        this.entityStateAccessorsFactory = entityStateAccessorsFactory;
    }
	
	// Introduced fields
	private Relationship RelationshipBacked.underlyingRelationship;
    private EntityStateAccessors<RelationshipBacked,Relationship> RelationshipBacked.stateAccessors;

	public void RelationshipBacked.setUnderlyingState(Relationship r) {
        this.underlyingRelationship = r;
        if (this.stateAccessors == null) {
            this.stateAccessors = Neo4jRelationshipBacking.aspectOf().entityStateAccessorsFactory.getEntityStateAccessors(this);
        } else {
            this.stateAccessors.setUnderlyingState(r);
        }
	}
	
	public Relationship RelationshipBacked.getUnderlyingState() {
		return underlyingRelationship;
	}
	public boolean RelationshipBacked.hasUnderlyingRelationship() {
		return underlyingRelationship!=null;
	}

	public Long RelationshipBacked.getId() {
        if (!hasUnderlyingRelationship()) return null;
		return underlyingRelationship.getId();
	}
	
	
	//-------------------------------------------------------------------------
	// Equals and hashCode for Neo4j entities.
	// Final to prevent overriding.
	//-------------------------------------------------------------------------
	// TODO could use template method for further checks if needed
	public final boolean RelationshipBacked.equals(Object obj) {
		if (obj instanceof RelationshipBacked) {
			return this.getUnderlyingState().equals(((RelationshipBacked) obj).getUnderlyingState());
		}
		return false;
	}
	
	public final int RelationshipBacked.hashCode() {
		return getUnderlyingState().hashCode();
	}


    Object around(RelationshipBacked entity): entityFieldGet(entity) {
        Object result=entity.stateAccessors.getValue(field(thisJoinPoint));
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    Object around(RelationshipBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        Object result=entity.stateAccessors.setValue(field(thisJoinPoint),newVal);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity,result);
	}


    Field field(JoinPoint joinPoint) {
        FieldSignature fieldSignature = (FieldSignature)joinPoint.getSignature();
        return fieldSignature.getField();
    }
}
