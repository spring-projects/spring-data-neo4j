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
package org.springframework.data.falkordb.core;

import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

import org.springframework.data.domain.Sort;

/**
 * Interface that specifies a basic set of FalkorDB operations. Provides graph-specific
 * operations for working with nodes and relationships.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface FalkorDBOperations {

	/**
	 * Saves an entity to the graph. Use this to save individual entities and their
	 * relations to the graph.
	 * @param <T> the type of the entity
	 * @param instance the entity to save
	 * @return the saved entity
	 */
	<T> T save(T instance);

	/**
	 * Saves multiple entities to the graph.
	 * @param <T> the type of the entities
	 * @param instances the entities to save
	 * @return the saved entities
	 */
	<T> List<T> saveAll(Iterable<T> instances);

	/**
	 * Loads an entity from the graph.
	 * @param <T> the type of the entity
	 * @param id the id of the entity
	 * @param clazz the class of the entity
	 * @return the loaded entity
	 */
	<T> Optional<T> findById(Object id, Class<T> clazz);

	/**
	 * Loads multiple entities by their ids.
	 * @param <T> the type of the entities
	 * @param ids the ids of the entities
	 * @param clazz the class of the entities
	 * @return the loaded entities
	 */
	<T> List<T> findAllById(Iterable<?> ids, Class<T> clazz);

	/**
	 * Loads all entities of a given type.
	 * @param <T> the type of the entities
	 * @param clazz the class of the entities
	 * @return all entities of the given type
	 */
	<T> List<T> findAll(Class<T> clazz);

	/**
	 * Loads all entities of a given type with sorting.
	 * @param <T> the type of the entities
	 * @param clazz the class of the entities
	 * @param sort the sorting specification
	 * @return all entities of the given type sorted
	 */
	<T> List<T> findAll(Class<T> clazz, Sort sort);

	/**
	 * Counts entities of a given type.
	 * @param <T> the type of the entities
	 * @param clazz the class of the entities
	 * @return the number of entities
	 */
	<T> long count(Class<T> clazz);

	/**
	 * Checks if an entity with the given id exists.
	 * @param <T> the type of the entity
	 * @param id the id to check
	 * @param clazz the class of the entity
	 * @return {@literal true} if the entity exists
	 */
	<T> boolean existsById(Object id, Class<T> clazz);

	/**
	 * Deletes an entity by its id.
	 * @param <T> the type of the entity
	 * @param id the id of the entity
	 * @param clazz the class of the entity
	 */
	<T> void deleteById(Object id, Class<T> clazz);

	/**
	 * Deletes multiple entities by their ids.
	 * @param <T> the type of the entities
	 * @param ids the ids of the entities
	 * @param clazz the class of the entities
	 */
	<T> void deleteAllById(Iterable<?> ids, Class<T> clazz);

	/**
	 * Deletes all entities of a given type.
	 * @param <T> the type of the entities
	 * @param clazz the class of the entities
	 */
	<T> void deleteAll(Class<T> clazz);

	/**
	 * Executes a custom Cypher query.
	 * @param <T> the type of the result
	 * @param cypher the Cypher query
	 * @param parameters the query parameters
	 * @param clazz the expected result type
	 * @return the query results
	 */
	<T> List<T> query(String cypher, java.util.Map<String, Object> parameters, Class<T> clazz);

	/**
	 * Executes a custom Cypher query returning a single result.
	 * @param <T> the type of the result
	 * @param cypher the Cypher query
	 * @param parameters the query parameters
	 * @param clazz the expected result type
	 * @return the query result
	 */
	<T> Optional<T> queryForObject(String cypher, java.util.Map<String, Object> parameters, Class<T> clazz);

}
