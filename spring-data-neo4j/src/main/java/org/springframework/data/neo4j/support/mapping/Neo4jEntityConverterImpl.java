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
package org.springframework.data.neo4j.support.mapping;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.TypeMapper;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.mapping.*;
import org.springframework.data.neo4j.mapping.ManagedEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.InvocationTargetException;

/**
 * @author mh
 * @since 07.10.11
 */
public class Neo4jEntityConverterImpl<T,S extends PropertyContainer> implements Neo4jEntityConverter<T,S> {
    private final Neo4jMappingContext mappingContext;
    private final ConversionService conversionService;
    private final EntityInstantiator<S> entityInstantiator;
    private final EntityStateHandler entityStateHandler;
    private final TypeMapper<S> typeMapper;
    private final SourceStateTransmitter<S> sourceStateTransmitter;
    private final Neo4jEntityFetchHandler entityFetchHandler;

    public Neo4jEntityConverterImpl(Neo4jMappingContext mappingContext, ConversionService conversionService,
                                    EntityStateHandler entityStateHandler, Neo4jEntityFetchHandler entityFetchHandler,
                                    EntityTools<S> entityTools) {
        this.mappingContext = mappingContext;
        this.conversionService = conversionService;
        this.entityStateHandler = entityStateHandler;
        this.entityFetchHandler = entityFetchHandler;
        this.entityInstantiator = new Neo4jEntityPersister.CachedInstantiator<S>(entityTools.getEntityInstantiator());
        this.typeMapper = entityTools.getTypeMapper();
        this.sourceStateTransmitter = entityTools.getSourceStateTransmitter();
    }

    @Override
    public MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> getMappingContext() {
        return mappingContext;
    }

    @Override
    public ConversionService getConversionService() {
        return conversionService;
    }

    @Override
    public <R extends T> R read(Class<R> requestedType, S source, MappingPolicy mappingPolicy, final Neo4jTemplate template) {
        // 1) source -> type alias
        // 2) type alias -> type
        // 3) check for subtype matching / enforcement
        final TypeInformation<R> requestedTypeInformation = requestedType == null ? null : ClassTypeInformation.from(requestedType);
        final TypeInformation<? extends R> targetType = typeMapper.readType(source, requestedTypeInformation);

        // retrieve meta-information about the type
        @SuppressWarnings("unchecked") final Neo4jPersistentEntityImpl<R> persistentEntity = (Neo4jPersistentEntityImpl<R>) mappingContext.getPersistentEntity(targetType);

        if (mappingPolicy==null) {
            mappingPolicy = persistentEntity.getMappingPolicy();
        }

        // 4) create object instance
        final R createdEntity = entityInstantiator.createEntityFromState(source, targetType.getType(), mappingPolicy);

        // 5) connect state
        entityStateHandler.setPersistentState(createdEntity,source);

        if (persistentEntity.isManaged()) return createdEntity;
        loadEntity(createdEntity, source, mappingPolicy, persistentEntity, template);
        return createdEntity;
    }

    @Override
    public <R extends T> R loadEntity(R entity, S source, MappingPolicy mappingPolicy, Neo4jPersistentEntityImpl<R> persistentEntity, final Neo4jTemplate template) {
        if (mappingPolicy.shouldLoad()) {
            final BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper = BeanWrapper.<Neo4jPersistentEntity<R>, R>create(entity, conversionService);
            sourceStateTransmitter.copyPropertiesFrom(wrapper, source, persistentEntity,mappingPolicy, template);
            // 6) handle cascading fetches
            cascadeFetch(persistentEntity, wrapper, mappingPolicy, template);
        }
        return entity;
    }

    private <R extends T> void cascadeFetch(Neo4jPersistentEntityImpl<R> persistentEntity, final BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, final MappingPolicy policy, final Neo4jTemplate template) {
        persistentEntity.doWithAssociations(new AssociationHandler<Neo4jPersistentProperty>() {
            @Override
            public void doWithAssociation(Association<Neo4jPersistentProperty> association) {
                final Neo4jPersistentProperty property = association.getInverse();
                // MappingPolicy mappingPolicy = policy.combineWith(property.getMappingPolicy());
                final MappingPolicy mappingPolicy = property.getMappingPolicy();
                if (mappingPolicy.shouldLoad() && property.isRelationship()) {
                    final Object value = getProperty(wrapper, property);
                    @SuppressWarnings("unchecked") final Neo4jPersistentEntityImpl<Object> persistentEntity =
                            (Neo4jPersistentEntityImpl<Object>) mappingContext.getPersistentEntity(property.getTypeInformation().getActualType());
                    final Object fetchedValue = entityFetchHandler.fetch(value, persistentEntity, property, mappingPolicy, template);
                    // replace fetched one-time iterables and similiar managed values
                    sourceStateTransmitter.setProperty(wrapper, property, fetchedValue);
                }
            }
        });
    }

    private <R> Object getProperty(BeanWrapper<Neo4jPersistentEntity<R>, R> wrapper, Neo4jPersistentProperty property) {
        try {
            return wrapper.getProperty(property);
        } catch (IllegalAccessException e) {
            throw new MappingException("Error retrieving property " + property.getName() + " from " + wrapper.getBean(), e);
        } catch (InvocationTargetException e) {
            throw new MappingException("Error retrieving property " + property.getName() + " from " + wrapper.getBean(), e.getTargetException());
        }
    }

    @Override
    public void write(T source, S sink, MappingPolicy mappingPolicy, final Neo4jTemplate template) {
        final Class<?> sourceType = source.getClass();
        @SuppressWarnings("unchecked") final Neo4jPersistentEntityImpl<T> persistentEntity = (Neo4jPersistentEntityImpl<T>) mappingContext.getPersistentEntity(sourceType);
        if (persistentEntity.isManaged()) { // todo check if typerepreentationstragegy is called ??
            ((ManagedEntity)source).persist();
            return;
        }

        final BeanWrapper<Neo4jPersistentEntity<T>, T> wrapper = BeanWrapper.<Neo4jPersistentEntity<T>, T>create(source, conversionService);
        if (sink == null) {
            sink = entityStateHandler.useOrCreateState(source,sink); // todo handling of changed state
            entityStateHandler.setPersistentState(source, sink);
            typeMapper.writeType(sourceType, sink);
        }
        sourceStateTransmitter.copyPropertiesTo(wrapper, sink, persistentEntity,mappingPolicy, template);
    }
/*
    private Node useGetOrCreateNode(S node, Neo4jPersistentEntity<?> persistentEntity, BeanWrapper<Neo4jPersistentEntity<Object>, Object> wrapper) {
        if (node != null) return node;
        final Neo4jPersistentProperty idProperty = persistentEntity.getIdProperty();
        final Long id = getProperty(wrapper, idProperty, Long.class, true);
        if (id == null) {
            final Node newNode = getTemplate().createNode();
            setProperty(wrapper, idProperty, newNode.getId());
            return newNode;
        }
        try {
            return getTemplate().getNodeById(id);
        } catch (NotFoundException nfe) {
            throw new MappingException("Could not find node with id " + id);
        }
    }
*/

}
