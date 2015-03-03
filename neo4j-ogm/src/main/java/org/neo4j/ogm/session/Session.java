/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.session;

import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.transaction.Transaction;

import java.util.Collection;
import java.util.Map;

public interface Session {

    <T> T load(Class<T> type, Long id);

    <T> T load(Class<T> type, Long id, int depth);

    <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids);

    <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth);

    <T> Collection<T> loadAll(Class<T> type);

    <T> Collection<T> loadAll(Class<T> type, int depth);

    <T> Collection<T> loadAll(Collection<T> objects);

    <T> Collection<T> loadAll(Collection<T> objects, int depth);

    <T> Collection<T> loadByProperty(Class<T> type, Property<String, Object> property);

    <T> Collection<T> loadByProperty(Class<T> type, Property<String, Object> property, int depth);


    void execute(String jsonStatements);

    void purgeDatabase();

    void clear();

    <T> void save(T object);

    <T> void save(T object, int depth);

    <T> void delete(T object);

    <T> void deleteAll(Class<T> type);


    Transaction beginTransaction();

    /**
     * Given a non modifying cypher statement this method will return a domain object that is hydrated to the
     * level specified in the given cypher query or a scalar (depending on the parametrized type).
     *
     * @param objectType The type that should be returned from the query.
     * @param cypher The parametrizable cypher to execute.
     * @param parameters Any scalar parameters to attach to the cypher.
     *
     * @param <T> A domain object or scalar.
     *
     * @return An instance of the objectType that matches the given cypher and parameters. Null if no object
     * is matched
     *
     * @throws java.lang.RuntimeException If more than one object is found.
     */
    <T> T queryForObject(Class<T> objectType, String cypher,  Map<String, Object> parameters);

    /**
     * Given a non modifying cypher statement this method will return a collection of domain objects that is hydrated to
     * the level specified in the given cypher query or a collection of scalars (depending on the parametrized type).
     *
     * @param objectType The type that should be returned from the query.
     * @param cypher The parametrizable cypher to execute.
     * @param parameters Any parameters to attach to the cypher.
     *
     * @param <T> A domain object or scalar.
     *
     * @return A collection of domain objects or scalars as prescribed by the parametrized type.
     */
    <T> Iterable<T> query(Class<T> objectType, String cypher, Map<String, Object> parameters);

    /**
     * Given a non modifying cypher statement this method will return a collection of Map's which represent Neo4j
     * objects as properties.
     *
     * Each element is a map which you can access by the name of the returned field
     *
     * TODO: Decide if we want to keep this behaviour?
     * TODO: Are we going to use the neo4jOperations conversion method to cast the value object to its proper class?
     *
     * @param cypher  The parametrizable cypher to execute.
     * @param parameters Any parameters to attach to the cypher.
     *
     * @return An iterable collection of Maps with each entry representing a neo4j object's properties.
     */
    Iterable<Map<String, Object>> query(String cypher, Map<String, Object> parameters);

    /**
     * This method allows a cypher statement with a modification statement to be executed.
     *
     * <p>Parameters may be scalars or domain objects themselves.</p>
     *
     * @param cypher The parametrizable cypher to execute.
     * @param parameters Any parameters to attach to the cypher. These may be domain objects or scalars. Note that
     *                   if a complex domain object is provided only the properties of that object will be set.
     *                   If relationships of a provided object also need to be set then the cypher should reflect this
     *                   and further domain object parameters provided.
     *
     */
    void execute(String cypher, Map<String, Object> parameters);
}
