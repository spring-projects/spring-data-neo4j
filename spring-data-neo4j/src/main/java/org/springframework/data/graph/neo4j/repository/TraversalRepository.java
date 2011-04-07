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

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;

/**
 * @author mh
 * @since 29.03.11
 */
public interface TraversalRepository<T extends GraphBacked<?>> {
    /**
     * Traversal based finder that returns a lazy Iterable over the traversal results
     *
     * @param startNode            the node to start the traversal from
     * @param traversalDescription
     * @param <N>                  Start node entity type
     * @return Iterable over traversal result
     */
    <N extends NodeBacked> Iterable<T> findAllByTraversal(N startNode, TraversalDescription traversalDescription);
}
