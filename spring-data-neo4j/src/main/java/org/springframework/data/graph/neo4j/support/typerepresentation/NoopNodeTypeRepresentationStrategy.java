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

package org.springframework.data.graph.neo4j.support.typerepresentation;

import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeRepresentationStrategy;

public class NoopNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    @Override
    public void postEntityCreation(Node state, Class<? extends NodeBacked> type) {
    }

    @Override
    public <U extends NodeBacked> ClosableIterable<U> findAll(Class<U> clazz) {
        throw new UnsupportedOperationException("findAll not supported.");
    }

    @Override
    public long count(Class<? extends NodeBacked> entityClass) {
        throw new UnsupportedOperationException("count not supported.");
    }

    @Override
    public void preEntityRemoval(Node state) {
    }

    @Override
    public Class<? extends NodeBacked> getJavaType(Node state) {
        throw new UnsupportedOperationException("getJavaType not supported.");
    }

    @Override
    public <U extends NodeBacked> U createEntity(Node state) {
        throw new UnsupportedOperationException("Creation with stored type not supported.");
    }

    @Override
    public <U extends NodeBacked> U createEntity(Node state, Class<U> type) {
        return projectEntity(state, type);
    }

    @Override
    public <U extends NodeBacked> U projectEntity(Node state, Class<U> type) {
        return null;
    }
}
