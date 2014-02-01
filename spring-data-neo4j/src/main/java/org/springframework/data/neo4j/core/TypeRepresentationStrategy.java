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

package org.springframework.data.neo4j.core;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

/**
 * Strategy to handle representation of java types in the graph. Possible implementation are type/class nodes
 * (forming an inheritance chain) that is linked to from the instance and keeps a count of the instances. Another
 * approach could use indexing and a type property on the instance fields.
 *
 * Contains a callback on entity creation that can set up the type representation. The finder methods are delegated to
 * the appropriate calls for the strategy set for the datastore.
 *
 * TODO use SDC TypeMapper implementations to delegate to for concrete implementation
 *
* @author Michael Hunger
* @since 13.09.2010
*/
public interface TypeRepresentationStrategy<S extends PropertyContainer> {
    /**
     * Callback for setting up and/or storing type information after creation.
     *
     * @param state Backing state of entity being created
     * @param type Type of entity being created
     */
    void writeTypeTo(S state, StoredEntityType type);

    /**
     *
     *
     * @param type
     * @return lazy Iterable over all instances of the given type
     */
    <U> ClosableIterable<S> findAll(final StoredEntityType type);

    /**
     *
     * @param type
     * @return number of instances of this class contained in the graph
     */
    long count(final StoredEntityType type);

    /**
     *
     * @return java type that of the node entity of this node
     */
	Object readAliasFrom(S state);

    /**
     * Callback for cleaning up type information before removal. If state does not have any
     * state associated, doesn't do anything.
     *
     * @param state Backing state of entity being removed
     */
    void preEntityRemoval(S state);

    boolean isLabelBased();
}
