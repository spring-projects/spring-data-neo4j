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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Relationship;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipEntityState<ENTITY extends RelationshipBacked> extends DefaultEntityState<ENTITY, Relationship> {

    private final GraphDatabaseContext graphDatabaseContext;
    
    private final DirectGraphRepositoryFactory graphRepositoryFactory;

    public RelationshipEntityState(final Relationship underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext, final DirectGraphRepositoryFactory graphRepositoryFactory) {
        super(underlyingState, entity, type, new DelegatingFieldAccessorFactory(graphDatabaseContext, graphRepositoryFactory) {
            @Override
            protected Collection<FieldAccessorListenerFactory<?>> createListenerFactories() {
                return Arrays.<FieldAccessorListenerFactory<?>>asList(
                        new IndexingPropertyFieldAccessorListenerFactory(
                		graphDatabaseContext,
                		new PropertyFieldAccessorFactory(graphDatabaseContext.getConversionService()),
                		new ConvertingNodePropertyFieldAccessorFactory(graphDatabaseContext.getConversionService())
                ));
            }

            @Override
            protected Collection<? extends FieldAccessorFactory<?>> createAccessorFactories() {
                return Arrays.<FieldAccessorFactory<?>>asList(
                        new TransientFieldAccessorFactory(),
                        new RelationshipNodeFieldAccessorFactory(graphDatabaseContext),
                        new PropertyFieldAccessorFactory(graphDatabaseContext.getConversionService()),
                        new ConvertingNodePropertyFieldAccessorFactory(graphDatabaseContext.getConversionService())
                );
            }
        });
        this.graphDatabaseContext = graphDatabaseContext;
        this.graphRepositoryFactory = graphRepositoryFactory;
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
