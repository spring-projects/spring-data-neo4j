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

package org.springframework.data.graph.neo4j.support.path;

import org.neo4j.graphdb.Path;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.EntityPath;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.path.PathMapper;

/**
 * @author mh
 * @since 26.02.11
 */
public abstract class EntityMapper<S extends NodeBacked, E extends NodeBacked, T> implements PathMapper<T> {
    private GraphDatabaseContext graphDatabaseContext;

    protected EntityMapper(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public abstract T mapPath(EntityPath<S,E> entityPath);

    @Override
    public T mapPath(Path path) {
        return mapPath(new EntityPath<S,E>(graphDatabaseContext, path));
    }

    public abstract static class WithoutResult<S extends NodeBacked,E extends NodeBacked> extends EntityMapper<S,E,Void> {
        protected WithoutResult(GraphDatabaseContext graphDatabaseContext) {
            super(graphDatabaseContext);
        }

        @Override
        public Void mapPath(EntityPath<S, E> entityPath) {
            doWithPath(entityPath);
            return null;
        }

        public abstract void doWithPath(EntityPath<S, E> entityPath);
    }
}
