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

package org.springframework.data.graph.neo4j.repository;

import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

/**
 * Simple Factory for {@link NodeGraphRepository} instances.
 */
public class DirectGraphRepositoryFactory {

    private final GraphDatabaseContext graphDatabaseContext;

    public DirectGraphRepositoryFactory(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public <T extends NodeBacked> GraphRepository<T> createNodeEntityRepository(Class<T> clazz) {
        return new NodeGraphRepository<T>(clazz, graphDatabaseContext);
    }

    public <T extends RelationshipBacked> GraphRepository<T> createRelationshipEntityRepository(Class<T> clazz) {
        return new RelationshipGraphRepository<T>(clazz, graphDatabaseContext);
    }

}
