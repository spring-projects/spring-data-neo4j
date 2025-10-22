/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
