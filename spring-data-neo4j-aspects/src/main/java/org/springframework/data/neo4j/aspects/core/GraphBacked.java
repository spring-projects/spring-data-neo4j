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

package org.springframework.data.neo4j.aspects.core;

import org.springframework.data.neo4j.mapping.ManagedEntity;

/**
 * super interface denoting entities that are graph backed, the backing STATE can be a {@link org.neo4j.graphdb.Node}
 * or a {@link org.neo4j.graphdb.Relationship}.
 * Subclasses of this interface bind the type parameter to a concrete Node or Relationship state.
 *
 * @author Michael Hunger
 * @since 21.09.2010
 */
public interface GraphBacked<STATE,ENTITY extends GraphBacked<STATE,ENTITY>> extends ManagedEntity<STATE,ENTITY> {
    /**
     * internal setter used for initializing the graph-db state on existing or newly created entities
     *
     * @param state (Node or Relationship)
     */
    void setPersistentState(STATE state);

    /**
     * @return the underlying graph-db state or null if the current entity is not related to the graph-store (possible with unsaved or partial entities)
     */
    STATE getPersistentState();

    boolean hasPersistentState();

    /**
     * removes the entity using @{link Neo4jTemplate.removeNodeEntity}
     * the entity and relationship are still accessible after removal but before transaction commit
     * but all modifications will throw an exception
     */
    void remove();
}
