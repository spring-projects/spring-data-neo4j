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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;

public class NodeEntityStateFactory {

	private GraphDatabaseContext graphDatabaseContext;
	
	private DirectGraphRepositoryFactory graphRepositoryFactory;

    private EntityManagerFactory entityManagerFactory;

	private NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory;

	public EntityState<NodeBacked,Node> getEntityState(final NodeBacked entity) {
        final NodeEntity graphEntityAnnotation = entity.getClass().getAnnotation(NodeEntity.class); // todo cache ??
        if (graphEntityAnnotation.partial()) {
            final PartialNodeEntityState<NodeBacked> partialNodeEntityState = new PartialNodeEntityState<NodeBacked>(null, entity, entity.getClass(), graphDatabaseContext, graphRepositoryFactory,getPersistenceUnitUtils());
            return new DetachedEntityState<NodeBacked, Node>(partialNodeEntityState, graphDatabaseContext) {
                @Override
                protected boolean isDetached() {
                    return super.isDetached() || partialNodeEntityState.getId(entity) == null;
                }
            };
        } else {
            NodeEntityState<NodeBacked> nodeEntityState = new NodeEntityState<NodeBacked>(null, entity, entity.getClass(), graphDatabaseContext, nodeDelegatingFieldAccessorFactory);
            // alternative was return new NestedTransactionEntityState<NodeBacked, Node>(nodeEntityState,graphDatabaseContext);
            return new DetachedEntityState<NodeBacked, Node>(nodeEntityState, graphDatabaseContext);
        }
    }

    private PersistenceUnitUtil getPersistenceUnitUtils() {
        if (entityManagerFactory == null) return null;
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    public void setNodeDelegatingFieldAccessorFactory(
			NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory) {
		this.nodeDelegatingFieldAccessorFactory = nodeDelegatingFieldAccessorFactory;
	}
	
	public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
		this.graphDatabaseContext = graphDatabaseContext;
	}

	public void setGraphRepositoryFactory(DirectGraphRepositoryFactory graphRepositoryFactory) {
		this.graphRepositoryFactory = graphRepositoryFactory;
	}

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }
}
