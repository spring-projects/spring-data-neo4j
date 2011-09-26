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
import org.neo4j.graphdb.Relationship;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.fieldaccess.DefaultEntityState;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.mapping.Neo4JPersistentEntity;
import org.springframework.data.neo4j.support.GraphDatabaseContext;


/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipEntityState<ENTITY extends RelationshipBacked> extends DefaultEntityState<ENTITY, Relationship> {

    private final GraphDatabaseContext graphDatabaseContext;
    
    public RelationshipEntityState(final Relationship underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext, final DelegatingFieldAccessorFactory<RelationshipBacked> delegatingFieldAccessorFactory, Neo4JPersistentEntity<ENTITY> persistentEntity) {
        super(underlyingState, entity, type, delegatingFieldAccessorFactory, persistentEntity);
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public void createAndAssignState() {
        if (entity.getPersistentState()!=null) return;
        try {
            final Object id = getIdFromEntity();
            if (id instanceof Number) {
                final Relationship relationship = graphDatabaseContext.getRelationshipById(((Number) id).longValue());
                setPersistentState(relationship);
                if (log.isInfoEnabled())
                    log.info("Entity reattached " + entity.getClass() + "; used Relationship [" + entity.getPersistentState() + "];");
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
    public ENTITY persist() {
        createAndAssignState();
        return entity;
    }
}
