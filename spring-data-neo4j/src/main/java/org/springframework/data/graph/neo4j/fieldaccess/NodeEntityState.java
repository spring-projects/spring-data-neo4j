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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.persistence.StateProvider;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class NodeEntityState<ENTITY extends NodeBacked> extends DefaultEntityState<ENTITY, Node> {

    private final GraphDatabaseContext graphDatabaseContext;

    public NodeEntityState(final Node underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext, final NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory) {
        super(underlyingState, entity, type, nodeDelegatingFieldAccessorFactory);
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public void createAndAssignState() {
        if (hasPersistentState()) {
            if (log.isInfoEnabled()) log.info("Entity "+entity.getClass()+" already has persistent state "+getPersistentState());
            return;
        }
        try {
            final Object id = getIdFromEntity();
            if (id instanceof Number) {
                final Node node = graphDatabaseContext.getNodeById(((Number) id).longValue());
                setPersistentState(node);
                if (log.isInfoEnabled())
                    log.info("Entity reattached " + entity.getClass() + "; used Node [" + getPersistentState() + "];");
                return;
            }

            final Node node = graphDatabaseContext.createNode();
            setPersistentState(node);
            if (log.isInfoEnabled()) log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + getPersistentState() + "]; Updating metamodel");
            graphDatabaseContext.postEntityCreation(entity);
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }

    @Override
    public ENTITY persist() {
        Node node = StateProvider.retrieveState();
        if (node != null) {
            setPersistentState(node);
        } else {
            createAndAssignState();
        }
        return entity;
    }
}
