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

package org.springframework.data.graph.core;

import org.neo4j.graphdb.Node;

/**
 * Strategy to handle representation of java types in the graph. Possible implementation are type/class nodes
 * (forming an inheritance chain) that is linked to from the instance and keeps a count of the instances. Another
 * approach could use indexing and a type property on the instance fields.
 *
 * Contains a callback on entity creation that can setup the type representation. The finder methods are delegated to
 * for the appropriate calls for the strategy set for the datastore.
 *
* @author Michael Hunger
* @since 13.09.2010
*/
public interface TypeRepresentationStrategy {
    /**
     * callback on entity creation for setting up type representation
     * @param entity
     */
    void postEntityCreation(NodeBacked entity);

    /**
     * @param clazz Type whose instances should be iterated over
     * @param <T> Type parameter for generified return value
     * @return lazy Iterable over all instances of the given type
     */
    <T extends NodeBacked> Iterable<T> findAll(final Class<T> clazz);

    /**
     * @param entityClass
     * @return number of instances of this class contained in the graph
     */
    long count(final Class<? extends NodeBacked> entityClass);

    /**
     * @param node
     * @param <T>
     * @return java type that of the node entity of this node
     */
	<T extends NodeBacked> Class<T> getJavaType(Node node);

    /**
     * callback for lifecycle management before node entity removal
     * @param entity
     */
    void preEntityRemoval(NodeBacked entity);

    <T extends NodeBacked> Class<T> confirmType(Node node, Class<T> type);
}
