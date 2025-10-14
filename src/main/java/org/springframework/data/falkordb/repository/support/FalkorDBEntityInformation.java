/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.falkordb.repository.support;

import org.springframework.data.repository.core.EntityInformation;

/**
 * FalkorDB-specific extension to {@link EntityInformation}.
 *
 * @param <T> the entity type
 * @param <ID> the ID type
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public interface FalkorDBEntityInformation<T, ID> extends EntityInformation<T, ID> {

	/**
	 * Returns the primary label of the entity.
	 * @return the primary label
	 */
	String getPrimaryLabel();

	/**
	 * Returns all labels associated with the entity.
	 * @return all labels
	 */
	String[] getLabels();

}
