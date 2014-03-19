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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mh
 * @since 08.10.11
 */
public class Neo4jEntityFetchHandler {
    private final SourceStateTransmitter<Node> nodeStateTransmitter;
    private final SourceStateTransmitter<Relationship> relationshipStateTransmitter;
    private final EntityStateHandler entityStateHandler;
    private final ConversionService conversionService;

    public Neo4jEntityFetchHandler(EntityStateHandler entityStateHandler, ConversionService conversionService, SourceStateTransmitter<Node> nodeStateTransmitter, SourceStateTransmitter<Relationship> relationshipStateTransmitter) {
        this.conversionService = conversionService;
        this.entityStateHandler = entityStateHandler;
        this.relationshipStateTransmitter = relationshipStateTransmitter;
        this.nodeStateTransmitter = nodeStateTransmitter;
    }


    // todo actually cascade !!
    public Object fetch(final Object value, Neo4jPersistentEntity<Object> persistentEntity, Neo4jPersistentProperty property, final MappingPolicy policy, final Neo4jTemplate template) {
        if (value == null) return value;
        //MappingPolicy mappingPolicy = mappingPolicy.combineWith(property.getMappingPolicy());
        final MappingPolicy mappingPolicy = property.getMappingPolicy();
        if (!mappingPolicy.shouldLoad()) return value;
        if (property.getTypeInformation().isCollectionLike()) {
            List<Object> replacement = new ArrayList<Object>();
            for (Object inner : ((Iterable) value)) {
                final BeanWrapper<Object> innerWrapper = BeanWrapper.create(inner, conversionService);
                final PropertyContainer state = entityStateHandler.getPersistentState(inner);
                fetchValue(innerWrapper, state, persistentEntity, mappingPolicy, template);
                replacement.add(inner);
                //sourceStateTransmitter.copyPropertiesFrom(innerWrapper, entityStateHandler.<S>getPersistentState(inner), persistentEntity);
            }
            return replacement;
        } else {
            final BeanWrapper<Object> innerWrapper = BeanWrapper.create(value, conversionService);
            final PropertyContainer state = entityStateHandler.getPersistentState(value);
            fetchValue(innerWrapper, state, persistentEntity, mappingPolicy, template);
//                        sourceStateTransmitter.copyPropertiesFrom(innerWrapper, entityStateHandler.<S>getPersistentState(value), persistentEntity);
        }
        return value;
    }
    public  void fetchValue(final BeanWrapper<Object> wrapper, PropertyContainer source, Neo4jPersistentEntity<Object> persistentEntity, final MappingPolicy mappingPolicy, final Neo4jTemplate template) {
        if (persistentEntity.isNodeEntity()) {
            nodeStateTransmitter.copyPropertiesFrom(wrapper, (Node) source,persistentEntity, mappingPolicy, template);
        }
        if (persistentEntity.isRelationshipEntity()) {
            relationshipStateTransmitter.copyPropertiesFrom(wrapper, (Relationship) source, persistentEntity, mappingPolicy, template);
        }
    }
}
