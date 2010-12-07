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
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import static org.springframework.data.graph.neo4j.fieldaccess.PartialNodeEntityStateAccessors.getId;

public class NodeEntityStateAccessorsFactory {

	private GraphDatabaseContext graphDatabaseContext;

	private NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory;

	public EntityStateAccessors<NodeBacked,Node> getEntityStateAccessors(final NodeBacked entity) {
        final NodeEntity graphEntityAnnotation = entity.getClass().getAnnotation(NodeEntity.class);
        if (graphEntityAnnotation!=null && graphEntityAnnotation.partial()) {
            return new DetachableEntityStateAccessors<NodeBacked, Node>(
                    new PartialNodeEntityStateAccessors<NodeBacked>(null, entity, entity.getClass(), graphDatabaseContext), graphDatabaseContext) {
                @Override
                protected boolean transactionIsRunning() {
                    return super.transactionIsRunning() && getId(entity, entity.getClass()) != null;
                }
            };
        } else {
            return new DetachableEntityStateAccessors<NodeBacked, Node>(
                    new NodeEntityStateAccessors<NodeBacked>(null,entity,entity.getClass(), graphDatabaseContext, nodeDelegatingFieldAccessorFactory),graphDatabaseContext);
        }
    }

	public void setNodeDelegatingFieldAccessorFactory(
			NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory) {
		this.nodeDelegatingFieldAccessorFactory = nodeDelegatingFieldAccessorFactory;
	}
	
	public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
		this.graphDatabaseContext = graphDatabaseContext;
	}
	
}
