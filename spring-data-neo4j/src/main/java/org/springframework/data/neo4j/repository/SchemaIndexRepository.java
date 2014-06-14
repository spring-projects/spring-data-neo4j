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

package org.springframework.data.neo4j.repository;

import org.springframework.data.neo4j.conversion.Result;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author Nicki Watt
 * @since 01.03.2014
 */
public interface SchemaIndexRepository<T> {

    /**
     * Finds an entity based on the provided schema indexed property value if one exists.
     * @param property The name of the schema indexed property
     * @param value The value of the schema indexed property
     * @return The single entity associated with this property value setting, or
     *         null if one does not exist.
     */
    @Transactional
    T findBySchemaPropertyValue(String property, Object value);

    /**
     * Finds all entities which have a schema indexed property set to specified value.
     * @param property The name of the schema indexed property
     * @param value The value of the schema indexed property
     * @return A result of all entities which match indexed property value.
     */
    @Transactional
    Result<T> findAllBySchemaPropertyValue(String property, Object value);

}
