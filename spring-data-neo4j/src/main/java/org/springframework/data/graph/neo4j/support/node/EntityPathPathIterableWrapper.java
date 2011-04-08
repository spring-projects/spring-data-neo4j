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

package org.springframework.data.graph.neo4j.support.node;

import org.neo4j.graphdb.Path;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.path.ConvertingEntityPath;
import org.springframework.data.graph.neo4j.support.path.EntityPath;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

/**
* @author mh
* @since 04.04.11
*/
public class EntityPathPathIterableWrapper<S extends NodeBacked, E extends NodeBacked> extends IterableWrapper<EntityPath<S,E>, Path> {
    private final GraphDatabaseContext graphDatabaseContext;

    public EntityPathPathIterableWrapper(Iterable<Path> paths, GraphDatabaseContext graphDatabaseContext) {
        super(paths);
        this.graphDatabaseContext = graphDatabaseContext;
    }

    protected EntityPath<S, E> underlyingObjectToObject(Path path) {
        return new ConvertingEntityPath<S,E>(graphDatabaseContext,path);
    }
}
