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
package org.springframework.data.neo4j.mapping;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.support.EntityInstantiator;
import org.springframework.data.neo4j.support.EntityStateHandler;
import org.springframework.data.neo4j.support.EntityTools;
import org.springframework.data.neo4j.support.ManagedEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 11.10.11
 */
public class Neo4jEntityPersister implements EntityPersister, Neo4jEntityConverter<Object,Node> {
    Neo4jEntityConverter<Object,Node> nodeConverter;
    Neo4jEntityConverter<Object,Relationship> relationshipConverter;
    private EntityStateHandler entityStateHandler;
    private final Neo4jMappingContext mappingContext;

    public Neo4jEntityPersister(ConversionService conversionService, EntityTools<Node> nodeEntityTools, EntityTools<Relationship> relationshipEntityTools, Neo4jMappingContext mappingContext, EntityStateHandler entityStateHandler) {
        this.mappingContext = mappingContext;
        this.entityStateHandler = entityStateHandler;

        Neo4jEntityFetchHandler fetchHandler=new Neo4jEntityFetchHandler(entityStateHandler, conversionService, nodeEntityTools.getSourceStateTransmitter(), relationshipEntityTools.getSourceStateTransmitter());

        this.nodeConverter = new CachedConverter<Node>(new Neo4jEntityConverterImpl<Object,Node>(mappingContext, conversionService, entityStateHandler, fetchHandler, nodeEntityTools));

        this.relationshipConverter = new CachedConverter<Relationship>(new Neo4jEntityConverterImpl<Object,Relationship>(mappingContext, conversionService, entityStateHandler, fetchHandler, relationshipEntityTools));

    }

    public <S extends PropertyContainer, T> T createEntityFromStoredType(S state) {
        return createEntityFromState(state,null);
    }


    static class StackedEntityCache {
        private long depth;
        private final Map<PropertyContainer,Object> objects =new HashMap<PropertyContainer, Object>();
        private static ThreadLocal<StackedEntityCache> stackedEntityCache = new ThreadLocal<StackedEntityCache>() {
            @Override
            protected StackedEntityCache initialValue() {
                return new StackedEntityCache();
            }
        };
        
        public static void push() {
            cache().depth++;
        }
        public static void pop() {
            if (--cache().depth==0) {
                stackedEntityCache.remove();
            }
        }
        @SuppressWarnings("unchecked")
        public static <T> T get(PropertyContainer state) {
            return (T) cache().objects.get(state);
        }
        public static <T> T add(PropertyContainer state, T value) {
            cache().objects.put(state, value);
            return value;
        }

        private static StackedEntityCache cache() {
            return stackedEntityCache.get();
        }

        public static boolean contains(PropertyContainer state) {
            return cache().objects.containsKey(state);
        }
    }
    public static class CachedInstantiator<S extends PropertyContainer> implements EntityInstantiator<S> {
        private final EntityInstantiator<S> delegate;

        public CachedInstantiator(EntityInstantiator<S> delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> T createEntityFromState(S state, Class<T> type) {
            try {
                if (state==null) throw new IllegalArgumentException("State must not be null");
                StackedEntityCache.push();
                if (StackedEntityCache.contains(state)) return StackedEntityCache.get(state);
                return StackedEntityCache.add(state, delegate.createEntityFromState(state, type));
            } finally {
                StackedEntityCache.pop();
            }
        }
    }
    public static class CachedConverter<S extends PropertyContainer> implements Neo4jEntityConverter<Object,S> {
        private final Neo4jEntityConverter<Object,S> delegate;

        public CachedConverter(Neo4jEntityConverter<Object, S> delegate) {
            this.delegate = delegate;
        }

        @Override
        public MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> getMappingContext() {
            return delegate.getMappingContext();
        }

        @Override
        public ConversionService getConversionService() {
            return delegate.getConversionService();
        }

        @Override
        public <R> R read(Class<R> type, S state) {
            try {
                if (state==null) throw new IllegalArgumentException("State must not be null");
                StackedEntityCache.push();
                if (StackedEntityCache.contains(state)) return StackedEntityCache.get(state);
                return StackedEntityCache.add(state, delegate.read(type, state));
            } finally {
                StackedEntityCache.pop();
            }
        }

        @Override
        public void write(Object source, S sink) {
            delegate.write(source,sink);
        }
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer, T> T createEntityFromState(S state, Class<T> type) {
        if (state == null) throw new IllegalArgumentException("state has to be either a Node or Relationship");
        if (isNode(state)) {
            return nodeConverter.read(type, (Node) state);
        }
        if (isRelationship(state)) {
            return relationshipConverter.read(type, (Relationship) state);
        }
        throw new IllegalArgumentException("state has to be either a Node or Relationship");
    }

    private boolean isRelationship(PropertyContainer state) {
        return state instanceof Relationship;
    }

    private boolean isNode(PropertyContainer state) {
        return state instanceof Node;
    }

    public <T> T projectTo(Object entity, Class<T> targetType) {
        PropertyContainer state = getPersistentState(entity);
        return createEntityFromState(state, targetType);
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> S getPersistentState(Object entity) {
        return entityStateHandler.getPersistentState(entity);
    }


    public Object persist(Object entity) {
        final Class<?> type = entity.getClass();
        if (isManaged(entity)) {
            return ((ManagedEntity)entity).persist();
        } else {
            return persist(entity, type);
        }
    }

    public boolean isManaged(Object entity) {
        return entityStateHandler.isManaged(entity);
    }

    private Object persist(Object entity, Class<?> type) {
        if (isNodeEntity(type)) {
            final Node node = this.<Node>getPersistentState(entity);
            this.nodeConverter.write(entity, node);
            return entity; // TODO ?
        }
        if (isRelationshipEntity(type)) {
            final Relationship relationship = this.<Relationship>getPersistentState(entity);
            this.relationshipConverter.write(entity, relationship);
            return entity; // TODO ?
        }
        throw new IllegalArgumentException("@NodeEntity or @RelationshipEntity annotation required on domain class"+type);
    }

    public boolean isNodeEntity(Class<?> targetType) {
        return mappingContext.isNodeEntity(targetType);
    }

    public boolean isRelationshipEntity(Class targetType) {
        return mappingContext.isRelationshipEntity(targetType);
    }

    public <S extends PropertyContainer> void setPersistentState(Object entity, S state) {
        entityStateHandler.setPersistentState(entity,state);
    }

    @Override
    public MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> getMappingContext() {
        return mappingContext;
    }

    @Override
    public ConversionService getConversionService() {
        return this.nodeConverter.getConversionService();
    }

    @Override
    public <R> R read(Class<R> type, Node source) {
        return createEntityFromState(source, type);
    }

    @Override
    public void write(Object source, Node sink) {
        nodeConverter.write(source,sink);
    }
}

