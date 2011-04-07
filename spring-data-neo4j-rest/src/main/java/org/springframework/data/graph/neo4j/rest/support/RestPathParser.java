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

package org.springframework.data.graph.neo4j.rest.support;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IterableWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 03.02.11
 */
public class RestPathParser {
    public static Path parse(Map path, final RestGraphDatabase restGraphDatabase) {
        final Collection<Map<?, ?>> nodesData = (Collection<Map<?, ?>>) path.get("nodes");
        final Collection<Map<?, ?>> relationshipsData = (Collection<Map<?, ?>>) path.get("relationships");
        final Map<?, ?> lastRelationshipData = lastElement(relationshipsData);
        final Map<?, ?> startData = (Map<?, ?>) path.get("start");
        final Map<?, ?> endData = (Map<?, ?>) path.get("end");
        final Integer length = (Integer) path.get("length");

        return new SimplePath(
                new RestNode(startData,restGraphDatabase),
                new RestNode(endData,restGraphDatabase),
                new RestRelationship(lastRelationshipData,restGraphDatabase),
                length,
                new IterableWrapper<Node, Map<?,?>>(nodesData) {
                    @Override
                    protected Node underlyingObjectToObject(Map<?, ?> data) {
                        return new RestNode(data,restGraphDatabase);
                    }
                },
                new IterableWrapper<Relationship, Map<?,?>>(relationshipsData) {
                    @Override
                    protected Relationship underlyingObjectToObject(Map<?, ?> data) {
                        return new RestRelationship(data,restGraphDatabase);
                    }
                });
    }

    private static Map<?, ?> lastElement(Collection<Map<?, ?>> collection) {
        if (collection.isEmpty()) return null;
        if (collection instanceof List) {
            List<Map<?,?>> list = (List<Map<?,?>>) collection;
            return list.get(list.size()-1);
        }
        Map<?, ?> result = null;
        for (Map<?, ?> value : collection) {
            result=value;
        }
        return result;
    }
}
