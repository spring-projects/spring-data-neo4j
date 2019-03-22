/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.repository;

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author mh
 * @since 29.03.11
 */
public interface TraversalRepository<T> {
    /**
     * Traversal based finder that returns a lazy Iterable over the traversal results
     *
     * @param startNode            the node to start the traversal from
     * @param traversalDescription
     * @param <N>                  Start node entity type
     * @return Iterable over traversal result
     */
    @Transactional
    <N> Iterable<T> findAllByTraversal(N startNode, TraversalDescription traversalDescription);
}
