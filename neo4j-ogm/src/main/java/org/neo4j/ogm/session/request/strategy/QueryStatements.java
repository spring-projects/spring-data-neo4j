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

package org.neo4j.ogm.session.request.strategy;

import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.cypher.query.GraphModelQuery;

import java.util.Collection;

public interface QueryStatements {

    /**
     * construct a query to fetch a single object with the specified id
     * @param id the id of the object to find
     * @param depth the depth to traverse for any related objects
     * @return a Cypher expression
     */
    GraphModelQuery findOne(Long id, int depth);

    /**
     * construct a query to fetch all objects with the specified ids
     * @param ids the ids of the objects to find
     * @param depth the depth to traverse for any related objects
     * @return a Cypher expression
     */
    GraphModelQuery findAll(Collection<Long> ids, int depth);

    /**
     * construct a query to fetch all objects
     * @return a Cypher expression
     */
    GraphModelQuery findAll();

    /**
     * construct a query to fetch all objects with the specified label
     * @param label the labels attached to the objects
     * @param depth the depth to traverse for related objects
     * @return a Cypher expression
     */
    GraphModelQuery findByLabel(String label, int depth);

    /**
     * construct a query to fetch all objects with the specified label and property
     * @param label the label value to filter on
     * @param property a property<K,V> value to filter on
     * @param depth the depth to traverse for related objects
     * @return a Cypher expression
     */
    GraphModelQuery findByProperty(String label, Property<String, Object> property, int depth);

}
