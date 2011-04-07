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

package org.springframework.data.graph.neo4j.support.node;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.persistence.AbstractConstructorEntityInstantiator;

/**
 * Implementation of an entity instantiator for neo4j graphdb nodes, binding the entity type to a NodeBacked and the
 * underlying state to a neo4j {@link Node}.
 * Part of the SPI, not intended for public use.
 * 
 * @author Rod Johnson
 */
public class NodeEntityInstantiator extends AbstractConstructorEntityInstantiator<NodeBacked, Node>{
	
	@Override
	protected void setState(NodeBacked entity, Node s) {
		entity.setPersistentState(s);
	}

    @Override
    protected String getFailingMessageForClass(Class<?> entityClass, Class<Node> stateClass) {
        return entityClass.getSimpleName() + ": entity must have a no-arg constructor.";
    }
}
