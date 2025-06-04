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
package org.springframework.data.neo4j.core.mapping.callback;

import org.apiguardian.api.API;
import org.neo4j.driver.types.MapAccessor;

import org.springframework.data.mapping.callback.EntityCallback;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * A callback that can be used to modify an instance of a {@link Neo4jPersistentEntity}
 * after it has been converted: That is, when a Neo4j record has been fully processed and
 * the entity and all its associations have been processed.
 * <p>
 * There is no reactive variant for this callback. It is safe to use this one for both
 * reactive and imperative workloads.
 *
 * @param <T> the type of the entity
 * @author Michael J. Simons
 * @since 6.3.0
 */
@FunctionalInterface
@API(status = STABLE, since = "6.3.0")
public interface AfterConvertCallback<T> extends EntityCallback<T> {

	/**
	 * Invoked after converting a Neo4j record (aka after hydrating an entity).
	 * @param instance the instance as hydrated by the
	 * {@link org.springframework.data.neo4j.core.mapping.Neo4jEntityConverter}.
	 * @param entity the entity definition
	 * @param source the Neo4j record that was used to hydrate the instance
	 * @return the domain object used further
	 */
	T onAfterConvert(T instance, Neo4jPersistentEntity<T> entity, MapAccessor source);

}
