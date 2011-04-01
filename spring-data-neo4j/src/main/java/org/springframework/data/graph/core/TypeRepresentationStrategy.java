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

import org.neo4j.graphdb.PropertyContainer;

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
public interface TypeRepresentationStrategy<S extends PropertyContainer, T extends GraphBacked<S>> {
    /**
     * Callback for setting up and/or storing type information after creation.
     *
     * @param state Backing state of entity being created
     * @param type Type of entity being created
     */
    void postEntityCreation(S state, Class<? extends T> type);

    /**
     * @param clazz Type whose instances should be iterated over
     * @param <U> Type parameter for generified return value
     * @return lazy Iterable over all instances of the given type
     */
    <U extends T> Iterable<U> findAll(final Class<U> clazz);

    /**
     * @param entityClass
     * @return number of instances of this class contained in the graph
     */
    long count(final Class<? extends T> entityClass);

    /**
     * @param state
     * @return java type that of the node entity of this node
     */
	<U extends T> Class<U> getJavaType(S state);

    /**
     * Callback for cleaning up type information before removal. If state does not have any
     * state associated, doesn't do anything.
     *
     * @param state Backing state of entity being removed
     */
    void preEntityRemoval(S state);

    /**
     * Instantiate the entity given its state. The type of the entity is inferred by the strategy
     * from the state.
     *
     * @param state Backing state of entity to be instantiated
     * @param <U> Helper parameter for castless use
     * @throws IllegalStateException If the strategy is unable to infer any type from the state
     * @return Entity instance
     */
    <U extends T> U createEntity(S state) throws IllegalStateException;

    /**
     * Instantiate the entity given its state. The type of the desired entity is also specified.
     * If the type is not compatible with what the strategy can infer from the state,
     * {@link java.lang.IllegalArgumentException} is thrown.
     *
     * @param state Backing state of entity to be instantiated
     * @param type Type of entity to be instantiated
     * @throws IllegalStateException If the strategy is unable to infer any type from the state
     * @throws IllegalArgumentException If the specified type does not match the inferred type
     * @return Entity instance
     */
    <U extends T> U createEntity(S state, Class<U> type) throws IllegalStateException, IllegalArgumentException;

    /**
     * Instantiate the entity of the given type, with the given state as backing state. No checking
     * is done by the strategy.
     *
     * @param state Backing state of entity to be instantiated
     * @param type Type of entity to be instantiated
     * @return Entity instance.
     */
    <U extends T> U projectEntity(S state, Class<U> type);
}
