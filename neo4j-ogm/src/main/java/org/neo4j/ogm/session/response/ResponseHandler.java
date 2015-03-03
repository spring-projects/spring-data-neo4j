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
