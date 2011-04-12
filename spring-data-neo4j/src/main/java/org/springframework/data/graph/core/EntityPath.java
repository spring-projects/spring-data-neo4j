/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.core;

import org.neo4j.graphdb.Path;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;

/**
 * @author mh
 * @since 08.04.11
 */
public interface EntityPath<S extends NodeBacked,E extends NodeBacked> extends Path {
    <T extends NodeBacked> T startEntity(Class<T>... types);

    <T extends NodeBacked> T endEntity(Class<T>... types);

    <T extends RelationshipBacked> T lastRelationshipEntity(Class<T>... types);

    <T extends NodeBacked> Iterable<T> nodeEntities();

    <T extends RelationshipBacked> Iterable<T> relationshipEntities(Class<T>... relationships);

    <T extends GraphBacked> Iterable<T> allPathEntities(Class<T>... relationships);
}
