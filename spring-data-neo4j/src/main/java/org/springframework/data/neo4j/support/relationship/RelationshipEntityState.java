/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.support.relationship;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.fieldaccess.DefaultEntityState;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipProperties;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.ParameterCheck;


/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipEntityState extends DefaultEntityState<Relationship> {

    private final Neo4jTemplate template;
    private MappingPolicy mappingPolicy;

    public RelationshipEntityState(final Relationship underlyingState, final Object entity, final Class<? extends Object> type, final Neo4jTemplate template, final DelegatingFieldAccessorFactory delegatingFieldAccessorFactory, Neo4jPersistentEntity<Object> persistentEntity) {
        super(underlyingState, entity, type, delegatingFieldAccessorFactory, persistentEntity);
        this.template = template;
        this.mappingPolicy = persistentEntity.getMappingPolicy();
    }

    @Override
    public void createAndAssignState() {
        final PropertyContainer state = template.getPersistentState(entity);
        if (state !=null) return;
        try {
            final Object id = getIdFromEntity();
            if (id instanceof Number) {
                final Relationship relationship = template.getRelationship(((Number) id).longValue());
                setPersistentState(relationship);
                if (log.isDebugEnabled())
                    log.debug("Entity reattached " + entity.getClass() + "; used Relationship [" + state + "];");
                return;
            }
            final Relationship relationship = createRelationshipFromEntity();
            setPersistentState(relationship);
            if (log.isDebugEnabled()) log.debug("User-defined constructor called on class " + entity.getClass() + "; created Relationship [" + getPersistentState() + "]; Updating metamodel");
            template.postEntityCreation(relationship, type);
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }

    private Relationship createRelationshipFromEntity() {
        final RelationshipProperties relationshipProperties = getPersistentEntity().getRelationshipProperties();
        final Node startNode = template.getPersistentState(relationshipProperties.getStartNodeProperty().getValue(entity, mappingPolicy));
        final Node endNode = template.getPersistentState(relationshipProperties.getEndNodeProperty().getValue(entity, mappingPolicy));
        final String type = getRelationshipTypeFromEntity(getPersistentEntity());
        ParameterCheck.notNull(startNode,"start node property",endNode,"end node property",type, "relationship type property from field or annotation");
        return template.createRelationshipBetween(startNode, endNode,type,null);
    }

    private String getRelationshipTypeFromEntity(Neo4jPersistentEntity<?> persistentEntity) {
        final RelationshipProperties relationshipProperties = persistentEntity.getRelationshipProperties();
        final Neo4jPersistentProperty typeProperty = relationshipProperties.getTypeProperty();
        final Object value = typeProperty!=null ? typeProperty.getValue(entity, mappingPolicy) : null;
        if (value==null) {
            return relationshipProperties.getRelationshipType();
        }
        if (value instanceof RelationshipType) {
            return ((RelationshipType)value).name();
        }
        return (String) value;
    }

    @Override
    public Object persist() {
        if (getPersistentState() == null) {
            createAndAssignState();
        }
        return entity;
    }
}
