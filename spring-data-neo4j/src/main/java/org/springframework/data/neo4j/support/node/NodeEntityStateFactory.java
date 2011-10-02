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
import org.springframework.data.neo4j.core.EntityState;

import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.DetachedEntityState;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

public class NodeEntityStateFactory {

	protected GraphDatabaseContext graphDatabaseContext;

    private DelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory;

    protected Neo4jMappingContext mappingContext;

    private boolean createDetachableEntities = true;

    public EntityState<Node> getEntityState(final Object entity) {
        final Class<?> entityType = entity.getClass();
        final NodeEntity graphEntityAnnotation = entityType.getAnnotation(NodeEntity.class); // todo cache ??
        final Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entityType);
        NodeEntityState nodeEntityState = new NodeEntityState(null, entity, entityType, graphDatabaseContext, nodeDelegatingFieldAccessorFactory, (Neo4jPersistentEntity) persistentEntity);
        // alternative was return new NestedTransactionEntityState<NodeBacked, Node>(nodeEntityState,graphDatabaseContext);
        if (createDetachableEntities) return new DetachedEntityState<Node>(nodeEntityState, graphDatabaseContext);
        return nodeEntityState;
    }

    public void setNodeDelegatingFieldAccessorFactory(
    		DelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory) {
		this.nodeDelegatingFieldAccessorFactory = nodeDelegatingFieldAccessorFactory;
	}
	
	public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
		this.graphDatabaseContext = graphDatabaseContext;
	}

    public Neo4jMappingContext getMappingContext() {
        return mappingContext;
    }

    public void setMappingContext(Neo4jMappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    public GraphDatabaseContext getGraphDatabaseContext() {
        return graphDatabaseContext;
    }

    public void setCreateDetachableEntities(boolean createDetachableEntities) {
        this.createDetachableEntities = createDetachableEntities;
    }
}
