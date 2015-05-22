/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.session.request.strategy;


import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Query;

import java.util.Collection;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public interface QueryStatements {

    /**
     * construct a query to fetch a single object with the specified id
     * @param id the id of the object to find
     * @param depth the depth to traverse for any related objects
     * @return a Cypher expression
     */
    Query findOne(Long id, int depth);

    /**
     * construct a query to fetch all objects
     * @return a Cypher expression
     */
    Query findAll();
    /**
     * construct a query to fetch all objects with the specified ids
     * @param ids the ids of the objects to find
     * @param depth the depth to traverse for any related objects
     * @return a Cypher expression
     */
    Query findAll(Collection<Long> ids, int depth);

    /**
     * construct queries to fetch all objects with the specified label or relationship type
     * @param type the label attached to the object, or the relationship type
     * @param depth the depth to traverse for related objects
     * @return a Cypher expression
     */
    Query findByType(String type, int depth);

    /**
     * construct queries to fetch all objects with the specified label and property
     * @param type the label value or relationship type to filter on
     * @param filters parameters to filter on
     * @param depth the depth to traverse for related objects
     * @return a Cypher expression
     */

    Query findByProperties(String type, Filters filters, int depth);

}
