/*
 * Copyright 2010 the original author or authors.
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
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.fieldaccess.PartialNodeEntityState.getId;

public class NodeEntityStateFactory {

	private GraphDatabaseContext graphDatabaseContext;
	
	private FinderFactory finderFactory;

	private NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory;

	public EntityState<NodeBacked,Node> getEntityState(final NodeBacked entity) {
        final NodeEntity graphEntityAnnotation = entity.getClass().getAnnotation(NodeEntity.class); // todo cache ??
        if (graphEntityAnnotation.partial()) {
            PartialNodeEntityState<NodeBacked> partialNodeEntityState = new PartialNodeEntityState<NodeBacked>(null, entity, entity.getClass(), graphDatabaseContext, finderFactory);
            return new DetachedEntityState<NodeBacked, Node>(partialNodeEntityState, graphDatabaseContext) {
                @Override
                protected boolean isDetached() {
                    return super.isDetached() || getId(entity, entity.getClass()) == null;
                }
            };
        } else {
            NodeEntityState<NodeBacked> nodeEntityState = new NodeEntityState<NodeBacked>(null, entity, entity.getClass(), graphDatabaseContext, nodeDelegatingFieldAccessorFactory);
            // alternative was return new NestedTransactionEntityState<NodeBacked, Node>(nodeEntityState,graphDatabaseContext);
            return new DetachedEntityState<NodeBacked, Node>(nodeEntityState, graphDatabaseContext);
        }
    }

	public void setNodeDelegatingFieldAccessorFactory(
			NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory) {
		this.nodeDelegatingFieldAccessorFactory = nodeDelegatingFieldAccessorFactory;
	}
	
	public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
		this.graphDatabaseContext = graphDatabaseContext;
	}

	public void setFinderFactory(FinderFactory finderFactory) {
		this.finderFactory = finderFactory;
	}

}
