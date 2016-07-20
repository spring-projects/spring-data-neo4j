/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.template;


import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.exception.NotFoundException;
import org.neo4j.ogm.model.Query;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.model.Result;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Data operations interface, implemented by {@link Neo4jTemplate}, that provides the API for using
 * the persistence framework in a more direct way as an alternative to the repositories.
 *
 * @author Adam George
 * @author Luanne Misquitta
 * @deprecated Use {@link org.neo4j.ogm.session.Session}
 */
@Repository
@Deprecated
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
     * Retrieves all the entities of the given class in the database hydrated to the specified depth, with Sorting.
     *
     * @param type          The type of entity to return.
     * @param sortOrder     The SortOrder to be applied
     * @param depth         The maximum number of relationships away from each loaded object to follow when loading related entities.
     *                      A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return A {@link Collection} containing all instances of the given type in the database or an empty collection if none
     *         are found, never <code>null</code>
     */
    <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, int depth);

    /**
     * Retrieves all the entities of the given class in the database hydrated to the specified depth, with Sorting and Pagination.
     *
     * @param type          The type of entity to return.
     * @param sortOrder     The SortOrder to be applied
     * @param pagination    The Pagination to be applied
     * @param depth         The maximum number of relationships away from each loaded object to follow when loading related entities.
     *                      A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return A {@link Collection} containing all instances of the given type in the database or an empty collection if none
     *         are found, never <code>null</code>
     */
    <T> Collection<T> loadAll(Class<T> type, SortOrder sortOrder, Pagination pagination, int depth);


    /**
     * Reloads all of the entities which match IDs in the given {@link Collection} to the specified depth.  Of course, this will
     * only work for persistent objects (i.e., those with a non-null <code>@GraphId</code> field).
     *
     * @param type  The type of entity to return.
     * @param ids     The IDs of objects to re-hydrate
     * @param depth   The depth to which the objects should be hydrated
     * @return A new {@link Collection} of entities matching those in the given collection hydrated to the given depth
     */
    <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth);

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
     * Reloads all of the entities in the given {@link Collection} to the specified depth, sorted by the sprcified SortOrder  Of course, this will
     * only work for persistent objects (i.e., those with a non-null <code>@GraphId</code> field).
     *
     * @param ids       The IDs of objects to re-hydrate
     * @param sortOrder The SortOrder to be used
     * @param depth     The depth to which the objects should be hydrated
     * @return A new {@link Collection} of entities matching those in the given collection hydrated to the given depth
     */
    <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, int depth);


    /**
     * Retrieves all the entities of the given class in the database hydrated to the default depth conforming to the specified
     * filter criteria.
     *
     * @param type 		The type of entity to return.
     * @param filter 	The filter to constrain entities with.
     * @return A {@link Collection} containing all matching instances of the given type in the database or an empty collection if none
     *         are found, never <code>null</code>
     */
    <T> Collection<T> loadAll(Class<T> type, Filter filter);


    /**
     * Retrieves all the entities of the given class in the database hydrated to the specified depth, with Pagination.
     *
     * @param type          The type of entity to return.
     * @param pagination    The Pagination to be applied
     * @param depth         The maximum number of relationships away from each loaded object to follow when loading related entities.
     *                      A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return A {@link Collection} containing all instances of the given type in the database or an empty collection if none
     *         are found, never <code>null</code>
     */
    <T> Collection<T> loadAll(Class<T> type, Pagination pagination, int depth);

    /**
     * Retrieves all the entities of the given class in the database hydrated to the default depth conforming to the specified
     * filter criteria.
     *
     * @param type 		The type of entity to return.
     * @param filter 	The filter to constrain entities with.
     * @param depth     The depth to which the objects should be hydrated
     * @return A {@link Collection} containing all matching instances of the given type in the database or an empty collection if none
     *         are found, never <code>null</code>
     */
    <T> Collection<T> loadAll(Class<T> type, Filter filter, int depth);


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
     * Retrieves the entity of the specified type that contains a property matching the given name with the given value.
     * This method assumes that the requested property/value combination will be unique for all entities of this type in
     * the database and will throw an exception unless exactly one result is found.  If several entities are expected to
     * be returned then use {@link #loadAllByProperty(Class, String, Object)} instead.
     *
     * @param type          The type of entity to load
     * @param propertyName  The name of the property on the entity against which to match the given value
     * @param propertyValue The value of the named property against which to match entities
     * @param depth         The maximum number of relationships away from each loaded object to follow when loading related entities.
     *                      A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return The instance of T corresponding to the entity that matches the given property, never <code>null</code>
     * @throws NotFoundException     if there are no matching entities
     * @throws IllegalStateException if there's more than one matching entity
     */
    <T> T loadByProperty(Class<T> type, String propertyName, Object propertyValue, int depth);

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
     * Retrieves all the entities of the specified type that contain a property matching the given name with the given value.
     *
     * @param type          The type of entity to load
     * @param propertyName  The name of the property on the entity against which to match the given value
     * @param propertyValue The value of the named property against which to match entities
     * @param depth         The maximum number of relationships away from each loaded object to follow when loading related entities.
     *                      A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return A {@link Collection} containing all the entities that match the given property or an empty {@link Collection} if
     *         there aren't any matches, never <code>null</code>
     */
    <T> Collection<T> loadAllByProperty(Class<T> type, String propertyName, Object propertyValue, int depth);

    /**
     * Retrieves the entity of the specified type that contains properties matching the ones supplied with given name and value.
     * This method assumes that the requested property/value combinations will be unique for all entities of this type in
     * the database and will throw an exception unless exactly one result is found.  If several entities are expected to
     * be returned then use {@link #loadAllByProperty(Class, String, Object)} instead.
     *
     * @param type          The type of entity to load
     * @param parameters    The parameters to filter by
     * @return The instance of T corresponding to the entity that matches the given properties, never <code>null</code>
     * @throws NotFoundException     if there are no matching entities
     * @throws IllegalStateException if there's more than one matching entity
     */
    <T> T loadByProperties(Class<T> type, Filters parameters);

    /**
     * Retrieves the entity of the specified type that contains properties matching the ones supplied with given name and value.
     * This method assumes that the requested property/value combinations will be unique for all entities of this type in
     * the database and will throw an exception unless exactly one result is found.  If several entities are expected to
     * be returned then use {@link #loadAllByProperty(Class, String, Object)} instead.
     *
     * @param type          The type of entity to load
     * @param parameters    The parameters to filter by
     * @param depth         The maximum number of relationships away from each loaded object to follow when loading related entities.
     *                      A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return The instance of T corresponding to the entity that matches the given properties, never <code>null</code>
     * @throws NotFoundException     if there are no matching entities
     * @throws IllegalStateException if there's more than one matching entity
     */
    <T> T loadByProperties(Class<T> type, Filters parameters, int depth);

    /**
     * Retrieves all the entities of the specified type that contain a properties matching the ones supplied with given name and value.
     *
     * @param type          The type of entity to load
     * @param parameters    The parameters to filter by
     * @return A {@link Collection} containing all the entities that match the given properties or an empty {@link Collection} if
     *         there aren't any matches, never <code>null</code>
     */
    <T> Collection<T> loadAllByProperties(Class<T> type, Filters parameters);

    /**
     * Retrieves all the entities of the specified type that contain a properties matching the ones supplied with given name and value.
     *
     * @param type          The type of entity to load
     * @param parameters    The parameters to filter by
     * @param depth         The maximum number of relationships away from each loaded object to follow when loading related entities.
     *                      A value of 0 just loads the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return A {@link Collection} containing all the entities that match the given properties or an empty {@link Collection} if
     *         there aren't any matches, never <code>null</code>
     */
    <T> Collection<T> loadAllByProperties(Class<T> type, Filters parameters, int depth);

    /**
     * Saves the specified entity in the graph database.  If the entity is currently transient then the persistent version of
     * the entity will be returned, containing its new graph ID.
     *
     * @param entity The entity to save
     * @return The saved entity
     */
    <T> T save(T entity);

    /**
     * Saves the specified entity in the graph database to a custom depth.  If the entity is currently transient then the persistent version of
     * the entity will be returned, containing its new graph ID.
     *
     * @param entity The entity to save
     * @param depth  The maximum number of relationships away from the entity to follow when saving related entities.
     *               A value of 0 just saves the object's properties and no related entities.  A value of -1 implies no depth limit.
     * @return The saved entity
     */
    <T> T save(T entity, int depth);


    /**
     * Removes the given node or relationship entity from the graph.  The entity is first removed
     * from all indexes and then deleted.
     *
     * @param entity The entity to delete
     */
    void delete(Object entity);

	/**
     * Removes all nodes or relationship entities of a specific type from the graph.
     * @param type the type of entity to delete
     */
    <T> void deleteAll(Class<T> type);

    /**
     * Removes all mapping information from the current session
     */
    void clear();

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
     * @return A {@link Query} containing an {@link Iterable} map representing query results and {@link QueryStatistics} if applicable.
     * @throws RuntimeException if the given query is not a valid Cypherquery
     */
    Result query(String cypherQuery, Map<String, ?> params);

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
     * Given a cypher statement this method will return a {@link Query} object containing a collection of Map's which represent Neo4j
     * objects as properties, along with query statistics if applicable.
     *
     * Each element of the query result is a map which you can access by the name of the returned field
     *
     * TODO: Are we going to use the neo4jOperations conversion method to cast the value object to its proper class?
     *
     * @param cypher  The parametrisable cypher to execute.
     * @param parameters Any parameters to attach to the cypher.
     * @param readOnly true if the query is readOnly, false otherwise
     *
     * @return A {@link Query} of {@link Iterable}s with each entry representing a neo4j object's properties.
     */
    Result query(String cypher, Map<String, ?> parameters, boolean readOnly);

    /**
     * Provides the instance count for the given <em>node</em> entity type.  This method is also provided by the
     * corresponding repository.
     *
     * @param entityClass The {@link Class} representing the type of node entity to count
     * @return The number of entities in the database of the given type
     */
    long count(Class<?> entityClass);


//    /**
//     * Issue a single Cypher update operation (such as a <tt>CREATE</tt>, <tt>MERGE</tt> or <tt>DELETE</tt> statement).
//     *
//     * @deprecated Use {@link Neo4jOperations}.query() to return both results as well as query statistics.
//     * @param cypherQuery The Cypher query to execute
//     * @return {@link QueryStatistics} representing statistics about graph modifications as a result of the cypher execution.
//     */
//    @Deprecated
//    QueryStatistics execute(String cypherQuery);

//    /**
//     * Allows a cypher statement with a modification statement to be executed.
//     *
//     * <p>Parameters may be scalars or domain objects themselves.</p>
//     * @deprecated Use {@link Neo4jOperations}.query() to return both results as well as query statistics.
//     * @param cypher The parametrisable cypher to execute.
//     * @param parameters Any parameters to attach to the cypher. These may be domain objects or scalars. Note that
//     *                   if a complex domain object is provided only the properties of that object will be set.
//     *                   If relationships of a provided object also need to be set then the cypher should reflect this
//     *                   and further domain object parameters provided.
//     *
//     * @return {@link QueryStatistics} representing statistics about graph modifications as a result of the cypher execution.
//     */
//    @Deprecated
//    QueryStatistics execute(String cypher, Map<String, Object> parameters);

}
