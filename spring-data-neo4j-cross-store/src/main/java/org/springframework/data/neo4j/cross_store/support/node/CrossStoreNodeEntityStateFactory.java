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
package org.springframework.data.neo4j.cross_store.support.node;

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.fieldaccess.DetachedEntityState;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;

/**
 * @author mh
 * @since 30.09.11
 */
public class CrossStoreNodeEntityStateFactory extends NodeEntityStateFactory {
    private CrossStoreNodeEntityState.CrossStoreNodeDelegatingFieldAccessorFactory delegatingFieldAccessorFactory;
    private EntityManagerFactory entityManagerFactory;

    public EntityState<Node> getEntityState(final Object entity, boolean detachable) {
        final Class<?> entityType = entity.getClass();
        if (isPartial(entityType)) {
            final Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entityType);
            @SuppressWarnings("unchecked") final CrossStoreNodeEntityState<NodeBacked> partialNodeEntityState =
                    new CrossStoreNodeEntityState<NodeBacked>(null, (NodeBacked)entity, (Class<? extends NodeBacked>) entityType,
                            template, getPersistenceUnitUtils(), delegatingFieldAccessorFactory,
                            persistentEntity);
            if (!detachable) return partialNodeEntityState;
            return new DetachedEntityState<Node>(partialNodeEntityState, template) {
                @Override
                protected boolean isDetached() {
                    return super.isDetached() || partialNodeEntityState.getId(entity) == null;
                }
            };
        } else {
            return super.getEntityState(entity,detachable);
        }
    }

    private boolean isPartial(Class<?> entityType) {
        final NodeEntity graphEntityAnnotation = entityType.getAnnotation(NodeEntity.class); // todo cache ??
        return graphEntityAnnotation.partial();
    }

    private PersistenceUnitUtil getPersistenceUnitUtils() {
        if (entityManagerFactory == null|| !entityManagerFactory.isOpen()) return null;
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setTemplate(Neo4jTemplate template) {
        super.setTemplate(template);
         this.delegatingFieldAccessorFactory = new CrossStoreNodeEntityState.CrossStoreNodeDelegatingFieldAccessorFactory(template);
    }

}
