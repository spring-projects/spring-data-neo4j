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

package org.neo4j.ogm.session.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.ogm.cypher.compiler.CypherContext;
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.model.Property;

import java.util.Collection;
import java.util.Set;

public interface ResponseHandler {

    <T> T loadById(Class<T> type, Neo4jResponse<GraphModel> stream, Long id);
    <T> Collection<T> loadAll(Class<T> type, Neo4jResponse<GraphModel> stream);
    <T> Set<T> loadByProperty(Class<T> type, Neo4jResponse<GraphModel> stream, Property<String, Object> filter);

    void updateObjects(CypherContext context, Neo4jResponse<String> response, ObjectMapper mapper);
}
