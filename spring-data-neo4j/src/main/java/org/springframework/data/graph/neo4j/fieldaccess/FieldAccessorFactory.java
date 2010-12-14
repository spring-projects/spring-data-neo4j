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
 * Factory interface for a single field / field accessor. Provides means to check if a certain field is eligible for this
 * factory and also a factory method to create the field accessor.
 *
 * @author Michael Hunger
 * @since 12.09.2010
*/
public interface FieldAccessorFactory<E> {
    /**
     * @param f field to check
     * @return true if this factory is responsible for creating a accessor for this field
     */
    boolean accept(Field f);

    /**
     * @param f the field to create an accessor for
     * @return a field accessor for the field or null if none can be created
     */
    FieldAccessor<E,?> forField(Field f);
}
