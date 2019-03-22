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

package org.springframework.data.neo4j.fieldaccess;

import org.springframework.data.neo4j.mapping.MappingPolicy;

/**
 * interface for field accessors, encapsulates reading and writing from fields and write support information about the field.
 * It is used by the {@link org.springframework.data.neo4j.core.EntityState}.
 */
public interface FieldAccessor {

	/**
	 * Returns a default implementation for a field or {@code null} if none is provided.
	 * <p>
	 * This default implementation is returned in detached mode by the {@link DetachedEntityState}
	 * when the value of the field is get. 
	 * @return a default implementation for a field or {@code null} if none is provided.
	 */
	Object getDefaultValue();
	
    /**
     * handles field write modification.
     *
     * @param entity
     * @param newVal
     * @param mappingPolicy
     * @return the written value or a DoReturn wrapper with the written value or null.
     * DoReturn indicates that the aspect should not proceed to the original field access but instead return immediately.
     */
	Object setValue(Object entity, Object newVal, MappingPolicy mappingPolicy);

    /**
     *
     * @param entity
     * @param mappingPolicy
     * @return the value or a DoReturn wrapper with the value for the field.
     * DoReturn indicates that the aspect should not proceed to the original field access but instead return immediately.
     */
	Object getValue(Object entity, MappingPolicy mappingPolicy);

    /**
     * @param entity
     * @return false for read only or computed fields, true otherwise
     */
    boolean isWriteable(Object entity);
}
