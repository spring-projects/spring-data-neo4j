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

import org.springframework.data.graph.core.GraphBacked;

import java.lang.reflect.Field;

/**
 * Interface for classes encapsulating and delegating read and write field access of an GraphBacked entity to a number of field accessors.
 * Wraps the entity, the underlying state and also handles the creation of the state (call back).
 * @author Michael Hunger
 * @since 15.09.2010
 */
public interface EntityStateAccessors<ENTITY extends GraphBacked<STATE>,STATE> {
    ENTITY getEntity();

    void setUnderlyingState(STATE state);

    /**
     * @param field
     * @return value of the field either from the state and/or the entity
     */
    Object getValue(Field field);

    /**
     * @param field
     * @return true if the field can be written
     */
    boolean isWritable(Field field);

    /**
     * @param field
     * @param newVal
     * @return sets the value in the entity and/or the state
     */
    Object setValue(Field field, Object newVal);

    /**
     * callback for creating and initializing an initial state
     */
    void createAndAssignState();

    boolean hasUnderlyingState();
    STATE getUnderlyingState();
}
