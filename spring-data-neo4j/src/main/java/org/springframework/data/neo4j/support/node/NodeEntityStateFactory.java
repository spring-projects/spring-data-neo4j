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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.core.EntityState;

import org.springframework.data.neo4j.fieldaccess.DetachedEntityState;
import org.springframework.data.neo4j.fieldaccess.FieldAccessorFactoryFactory;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jPersistentEntityImpl;

public class NodeEntityStateFactory implements EntityStateFactory<Node> {

    private final static Logger log = LoggerFactory.getLogger(NodeEntityState.class);

    protected final FieldAccessorFactoryFactory nodeDelegatingFieldAccessorFactory;
    protected final Neo4jMappingContext mappingContext;

    public NodeEntityStateFactory(Neo4jMappingContext mappingContext, FieldAccessorFactoryFactory nodeDelegatingFieldAccessorFactory) {
        this.nodeDelegatingFieldAccessorFactory = nodeDelegatingFieldAccessorFactory;
        this.mappingContext = mappingContext;
    }

    public EntityState<Node> getEntityState(final Object entity, boolean detachable, Neo4jTemplate template) {
        final Class<?> entityType = entity.getClass();
        Neo4jPersistentEntity<Object> persistentEntity = getPersistentEntity(entityType);
        // if (persistentEntity==null) return null;
        NodeEntityState nodeEntityState = new NodeEntityState(null, entity, entityType, template,
                nodeDelegatingFieldAccessorFactory.provideFactoryFor(template), persistentEntity);
        if (!detachable) {
            return nodeEntityState;
        }
        return new DetachedEntityState<Node>(nodeEntityState, template);
    }

    protected Neo4jPersistentEntity<Object> getPersistentEntity(Class<?> entityType) {
        try {
            //noinspection unchecked
            return (Neo4jPersistentEntity<Object>) mappingContext.getPersistentEntity(entityType);
        } catch( MappingException e) {
            log.warn("Not able to resolve persistent entity mapping information for "+entityType,e);
            return null;
        }
    }
}
