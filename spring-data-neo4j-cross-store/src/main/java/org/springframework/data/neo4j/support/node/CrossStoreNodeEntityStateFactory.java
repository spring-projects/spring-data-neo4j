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
package org.springframework.data.neo4j.support.node;

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.fieldaccess.DetachedEntityState;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;

/**
 * @author mh
 * @since 30.09.11
 */
public class CrossStoreNodeEntityStateFactory extends NodeEntityStateFactory {
    private CrossStoreNodeEntityState.CrossStoreNodeDelegatingFieldAccessorFactory delegatingFieldAccessorFactory;
    private EntityManagerFactory entityManagerFactory;

    public EntityState<Node> getEntityState(final Object entity) {
        final Class<?> entityType = entity.getClass();
        final NodeEntity graphEntityAnnotation = entityType.getAnnotation(NodeEntity.class); // todo cache ??
        final Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entityType);
        if (graphEntityAnnotation.partial()) {
            final CrossStoreNodeEntityState<NodeBacked> partialNodeEntityState = new CrossStoreNodeEntityState<NodeBacked>(null, (NodeBacked)entity, (Class<? extends NodeBacked>) entityType, graphDatabaseContext, getPersistenceUnitUtils(), delegatingFieldAccessorFactory,  persistentEntity);
            return new DetachedEntityState<Node>(partialNodeEntityState, graphDatabaseContext) {
                @Override
                protected boolean isDetached() {
                    return super.isDetached() || partialNodeEntityState.getId(entity) == null;
                }
            };
        } else {
            NodeEntityState nodeEntityState = new NodeEntityState(null, entity, entityType, graphDatabaseContext, nodeDelegatingFieldAccessorFactory, (Neo4jPersistentEntity) persistentEntity);
            // alternative was return new NestedTransactionEntityState<NodeBacked, Node>(nodeEntityState,graphDatabaseContext);
            return new DetachedEntityState<Node>(nodeEntityState, graphDatabaseContext);
        }
    }

    private PersistenceUnitUtil getPersistenceUnitUtils() {
        if (entityManagerFactory == null|| !entityManagerFactory.isOpen()) return null;
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    private void setUp() {
         this.delegatingFieldAccessorFactory = new CrossStoreNodeEntityState.CrossStoreNodeDelegatingFieldAccessorFactory(graphDatabaseContext);
    }

}
