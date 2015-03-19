/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.template;

import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Data operations interface, implemented by {@link Neo4jTemplate}, that provides the API for using
 * the persistence framework in a more direct way as an alternative to the repositories.
 *
 * @author Adam George
 */
@Repository
public interface Neo4jOperations {

    /**
     * Loads an entity of type T that matches the specified ID to the default depth.
     *
     * @param type The type of entity to load
     * @param id   The ID of the node or relationship to match
     * @return The instance of T loaded from the database that matches the specified ID or <code>null</code> if no match is found
     */
    <T> T load(Class<T> type, Long id);

    /**
     * Loads an entity of type T that matches the specified ID to the given depth.
     *
     * @param type  The type of entity to load
     * @param id    The ID of the node or relationship to match
     * @param depth The maximum number of relationships away from the identified object to follow when loading related entities.
     *              A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return The instance of T loaded from the database that matches the specified ID or <code>null</code> if no match is found
     */
    <T> T load(Class<T> type, Long id, int depth);

    /**
     * Retrieves all the entities of the given class in the database hydrated to the default depth.
     *
     * @param type The type of entity to return.
     * @return A {@link Collection} containing all instances of the given type in the database or an empty collection if none
     *         are found, never <code>null</code>
     */
    <T> Collection<T> loadAll(Class<T> type);

    /**
     * Retrieves all the entities of the given class in the database hydrated to the specified depth.
     *
     * @param type  The type of entity to return.
     * @param depth The maximum number of relationships away from each loaded object to follow when loading related entities.
     *              A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return A {@link Collection} containing all instances of the given type in the database or an empty collection if none
     *         are found, never <code>null</code>
     */
    <T> Collection<T> loadAll(Class<T> type, int depth);

    /**
     * Reloads all of the entities in the given {@link Collection} to the specified depth.  Of course, this will
     * only work for persistent objects (i.e., those with a non-null <code>@GraphId</code> field).
     *
     * @param objects The objects to re-hydrate
     * @param depth   The depth to which the objects should be hydrated
     * @return A new {@link Collection} of entities matching those in the given collection hydrated to the given depth
     */
    <T> Collection<T> loadAll(Collection<T> objects, int depth);

    /**
     * Retrieves the entity of the specified type that contains a property matching the given name with the given value.
     * This method assumes that the requested property/value combination will be unique for all entities of this type in
     * the database and will throw an exception unless exactly one result is found.  If several entities are expected to
     * be returned then use {@link #loadAllByProperty(Class, String, Object)} instead.
     *
     * @param type          The type of entity to load
     * @param propertyName  The name of the property on the entity against which to match the given value
     * @param propertyValue The value of the named property against which to match entities
     * @return The instance of T corresponding to the entity that matches the given property, never <code>null</code>
     * @throws NotFoundException     if there are no matching entities
     * @throws IllegalStateException if there's more than one matching entity
     */
    <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue);

    /**
     * Retrieves all the entities of the specified type that contain a property matching the given name with the given value.
     *
     * @param type          The type of entity to load
     * @param propertyName  The name of the property on the entity against which to match the given value
     * @param propertyValue The value of the named property against which to match entities
     * @return A {@link Collection} containing all the entities that match the given property or an empty {@link Collection} if
     *         there aren't any matches, never <code>null</code>
     */
    <T> Collection<T> loadAllByProperty(Class<T> type, String propertyName, Object propertyValue);

    /**
     * Saves the specified entity in the graph database.  If the entity is currently transient then the persistent version of
     * the entity will be returned, containing its new graph ID.
     *
     * @param entity The entity to save
     * @return The saved entity
     */
    <T> T save(T entity);

    /**
     * Removes the given node or relationship entity from the graph.  The entity is first removed
     * from all indexes and then deleted.
     *
     * @param entity The entity to delete
     */
    void delete(Object entity);

    /**
     * Executes the specified Cypher query with the given parameters against the underlying Neo4j database and returns the
     * row-based result in the form of an {@link Iterable} of {@link Map}s.
     * <p>
     * Each of the resultant maps corresponds to a "row" in the result set and the key set in each map contains all the names
     * contained in the <code>RETURN</code> clause of the given query.
     * </p>
     *
     * @param cypherQuery The Cypher query to execute
     * @param params      The parameter to merge into the cypher query or an empty {@link Map} if the given query isn't parameterised
     * @return An {@link Iterable} of {@link Map}s represening the result of the query or an empty {@link Iterable} if there are
     *         no results, never <code>null</code>
     * @throws RuntimeException if the given query is not a valid Cypher <code>MATCH</code> query
     */
    Iterable<Map<String, Object>> query(String cypherQuery, Map<String, ?> params);

    /**
     * Runs the specified Cypher query with the given parameters against the underlying Neo4j database and returns the result
     * marshalled as an object of the requested type.
     *
     * @param entityType  The {@link Class} denoting the type of entity to return
     * @param cypherQuery The Cypher query to execute
     * @param parameters  The parameter to merge into the Cypher query or an empty {@link Map} if the query's not parameterised
     * @return An instance of T that corresponds to the entity found by executing the query or <code>null</code> if nothing is
     *         found by the query
     * @throws RuntimeException If more than one result is returned or there's an issue executing the query
     */
    <T> T queryForObject(Class<T> entityType, String cypherQuery, Map<String, ?> parameters);

    /**
     * Runs the specified Cypher query with the given parameters against the underlying Neo4j database and returns the result
     * marshalled as a group of objects of the requested type.
     *
     * @param entityType  The {@link Class} denoting the type of entity to return
     * @param cypherQuery The Cypher query to execute
     * @param parameters  The parameter to merge into the Cypher query or an empty {@link Map} if the query's not parameterised
     * @return An {@link Iterable} over the entities found by executing the query or an empty <code>Iterable</code> if nothing
     *         is found by the query, never <code>null</code>
     * @throws RuntimeException If there's an issue executing the query
     */
    <T> Iterable<T> queryForObjects(Class<T> entityType, String cypherQuery, Map<String, ?> parameters);

    /**
     * Issue a single Cypher update operation (such as a <tt>CREATE</tt>, <tt>MERGE</tt> or <tt>DELETE</tt> statement).
     *
     * @param cypherQuery The Cypher query to execute
     */
    void execute(String cypherQuery);

    /**
     * Provides the instance count for the given <em>node</em> entity type.  This method is also provided by the
     * corresponding repository.
     *
     * @param entityClass The {@link Class} representing the type of node entity to count
     * @return The number of entities in the database of the given type
     */
    long count(Class<?> entityClass);

}
