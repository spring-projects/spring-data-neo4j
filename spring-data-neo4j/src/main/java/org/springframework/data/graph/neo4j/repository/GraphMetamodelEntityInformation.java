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

package org.springframework.data.graph.neo4j.repository;

/**
 * @author mh
 * @since 28.03.11
 */

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.repository.support.AbstractEntityInformation;

public class GraphMetamodelEntityInformation<S extends PropertyContainer, T extends GraphBacked<S>> extends AbstractEntityInformation<T,Long> implements GraphEntityInformation<S,T> {

    private final boolean isNodeEntity;
    private final boolean isPartialEntity;
    private GraphDatabaseContext graphDatabaseContext;

    public GraphMetamodelEntityInformation(Class domainClass, GraphDatabaseContext graphDatabaseContext) {
        super(domainClass);
        this.graphDatabaseContext = graphDatabaseContext;
        NodeEntity nodeEntity = getJavaType().getAnnotation(NodeEntity.class);
        isNodeEntity = nodeEntity!=null;
        isPartialEntity = isNodeEntity && nodeEntity.partial();
    }

    @Override
    public boolean isNodeEntity() {
        return isNodeEntity;
    }

    @Override
    public boolean isPartialEntity() {
        return isPartialEntity;
    }

    @Override
    public boolean isNew(T entity) {
        return entity.hasPersistentState();
    }


    @Override
    public Long getId(T entity) {
        return isNodeEntity() ? ((NodeBacked)entity).getNodeId() : ((RelationshipBacked)entity).getRelationshipId();
    }

    @Override
    public Class<Long> getIdType() {
        return Long.class;
    }
}