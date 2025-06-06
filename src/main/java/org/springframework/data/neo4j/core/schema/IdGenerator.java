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
package org.springframework.data.neo4j.core.schema;

import org.apiguardian.api.API;

/**
 * Interface for generating ids for entities.
 *
 * @param <T> type of the id to generate
 * @author Michael J. Simons
 * @since 6.0
 */
@FunctionalInterface
@API(status = API.Status.STABLE, since = "6.0")
public interface IdGenerator<T> {

	/**
	 * Generates a new id for given entity.
	 * @param primaryLabel the primary label under which the entity is registered
	 * @param entity the entity to be saved
	 * @return id to be assigned to the entity
	 */
	T generateId(String primaryLabel, Object entity);

}
