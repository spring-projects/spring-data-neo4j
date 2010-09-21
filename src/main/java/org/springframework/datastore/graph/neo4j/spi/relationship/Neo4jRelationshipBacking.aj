package org.springframework.datastore.graph.neo4j.spi.relationship;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.*;
import org.springframework.datastore.graph.neo4j.fieldaccess.*;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.unwrap;

public aspect Neo4jRelationshipBacking extends AbstractTypeAnnotatingMixinFields<GraphRelationship,RelationshipBacked> {
	
	//-------------------------------------------------------------------------
	// Configure aspect for whole system.
	// init() method can be invoked automatically if the aspect is a Spring
	// bean, or called in user code.
	//-------------------------------------------------------------------------
	// Aspect shared Neo4J Graph Database Service
	private GraphDatabaseContext graphDatabaseContext;
    private RelationshipEntityStateAccessorsFactory entityStateAccessorsFactory;


	@Autowired
	public void init(GraphDatabaseContext graphDatabaseContext, RelationshipEntityStateAccessorsFactory relationshipEntityStateAccessorsFactory) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.entityStateAccessorsFactory = relationshipEntityStateAccessorsFactory;
    }
	
	// Introduced fields
	private Relationship RelationshipBacked.underlyingRelationship;
    private EntityStateAccessors<RelationshipBacked,Relationship> RelationshipBacked.underlyingState;

	public void RelationshipBacked.setUnderlyingState(Relationship r) {
        this.underlyingRelationship = r;
        if (this.underlyingState == null) {
            this.underlyingState = Neo4jRelationshipBacking.aspectOf().entityStateAccessorsFactory.getEntityStateAccessors(this);
        } else {
            this.underlyingState.setUnderlyingState(r);
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
        Object result=entity.underlyingState.getValue(field(thisJoinPoint));
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    Object around(RelationshipBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        Object result=entity.underlyingState.setValue(field(thisJoinPoint),newVal);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity,result);
	}


    Field field(JoinPoint joinPoint) {
        FieldSignature fieldSignature = (FieldSignature)joinPoint.getSignature();
        return fieldSignature.getField();
    }
}
