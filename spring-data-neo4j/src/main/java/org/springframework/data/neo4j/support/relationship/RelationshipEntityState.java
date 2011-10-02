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

import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import org.springframework.data.neo4j.fieldaccess.DefaultEntityState;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.support.GraphDatabaseContext;


/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipEntityState extends DefaultEntityState<Relationship> {

    private final GraphDatabaseContext graphDatabaseContext;
    
    public RelationshipEntityState(final Relationship underlyingState, final Object entity, final Class<? extends Object> type, final GraphDatabaseContext graphDatabaseContext, final DelegatingFieldAccessorFactory delegatingFieldAccessorFactory, Neo4jPersistentEntity<Object> persistentEntity) {
        super(underlyingState, entity, type, delegatingFieldAccessorFactory, persistentEntity);
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public void createAndAssignState() {
        final PropertyContainer state = graphDatabaseContext.getPersistentState(entity);
        if (state !=null) return;
        try {
            final Object id = getIdFromEntity();
            if (id instanceof Number) {
                final Relationship relationship = graphDatabaseContext.getRelationshipById(((Number) id).longValue());
                setPersistentState(relationship);
                if (log.isInfoEnabled())
                    log.info("Entity reattached " + entity.getClass() + "; used Relationship [" + state + "];");
                return;
            }

            final Relationship relationship = null; // TODO graphDatabaseContext.create();
            setPersistentState(relationship);
            if (log.isInfoEnabled()) log.info("User-defined constructor called on class " + entity.getClass() + "; created Relationship [" + getPersistentState() + "]; Updating metamodel");
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }

    @Override
    public Object persist() {
        createAndAssignState();
        return entity;
    }
}
