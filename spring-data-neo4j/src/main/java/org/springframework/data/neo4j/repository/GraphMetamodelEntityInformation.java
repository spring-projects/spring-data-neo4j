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

package org.springframework.data.neo4j.repository;

/**
 * @author mh
 * @since 28.03.11
 */

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelationshipEntity;



import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

public class GraphMetamodelEntityInformation<S extends PropertyContainer, T> extends AbstractEntityInformation<T,Long> implements GraphEntityInformation<S,T> {

    private GraphDatabaseContext graphDatabaseContext;
    private final RelationshipEntity relationshipEntity;
    private final NodeEntity nodeEntity;

    public GraphMetamodelEntityInformation(Class domainClass, GraphDatabaseContext graphDatabaseContext) {
        super(domainClass);
        this.graphDatabaseContext = graphDatabaseContext;
        nodeEntity = getJavaType().getAnnotation(NodeEntity.class);
        relationshipEntity = getJavaType().getAnnotation(RelationshipEntity.class);

    }

    @Override
    public boolean isNodeEntity() {
        return nodeEntity!=null;
    }

    @Override
    public boolean isPartialEntity() {
        return nodeEntity!=null && nodeEntity.partial();
    }

    @Override
    public boolean isRelationshipEntity() {
        return relationshipEntity!=null;
    }

    @Override
    public boolean isNew(T entity) {
        return graphDatabaseContext.getPersistentState(entity)!=null;
    }


    @Override
    public Long getId(T entity) {
        final PropertyContainer state = graphDatabaseContext.getPersistentState(entity);
        if (isNodeEntity()) {
            return ((Node)state).getId();
        }
        else {
            return ((Relationship) state).getId();
        }
    }

    @Override
    public Class<Long> getIdType() {
        return Long.class;
    }
}