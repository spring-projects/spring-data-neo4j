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

package org.neo4j.ogm.mapper;

import org.neo4j.ogm.model.GraphModel;

import java.util.Collection;

/**
 * Specification for an object-graph mapper, which can map {@link org.neo4j.ogm.model.GraphModel}s onto arbitrary Java objects.
 *
 * @param <G> The Graph implementation
 */
public interface GraphToEntityMapper<G extends GraphModel> {

    /**
     * Maps the data representation in the given {@link org.neo4j.ogm.model.GraphModel} onto an instance of <code>T</code>.
     *
     * @param graphModel The {@link org.neo4j.ogm.model.GraphModel} model containing the data to map onto the object
     * @return An object of type <code>T</code> containing relevant data extracted from the given graph model
     */
    <T> Collection<T> map(Class<T> type, G graphModel);

}
