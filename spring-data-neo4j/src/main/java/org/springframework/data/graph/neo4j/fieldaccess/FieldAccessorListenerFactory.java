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

import java.lang.reflect.Field;

/**
 * factory interface for field accessor listeners. Provides means to check if a field is eligible for this factory
 * and a factory method for creating the listener instance.
 *
 * @author Michael Hunger
 * @since 12.09.2010
 */
interface FieldAccessorListenerFactory<E> {
    /**
     * @param f field to check
     * @return true if this factory is able to create a listener for the field
     */
    boolean accept(Field f);

    /**
     * @param f field to create a listener for
     * @return newly created field listener
     */
    FieldAccessListener<E, ?> forField(Field f);
}
