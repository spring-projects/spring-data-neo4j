/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.finder;

import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

/**
 * Simple Factory for {@link NodeFinder} instances.
 */
public class FinderFactory {

    private final GraphDatabaseContext graphDatabaseContext;

    public FinderFactory(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public <T extends NodeBacked> NodeFinder<T> createNodeEntityFinder(Class<T> clazz) {
        return new NodeFinder<T>(clazz, graphDatabaseContext);
    }

    public <T extends RelationshipBacked> RelationshipFinder<T> createRelationshipEntityFinder(Class<T> clazz) {
        return new RelationshipFinder<T>(clazz, graphDatabaseContext);
    }

}
