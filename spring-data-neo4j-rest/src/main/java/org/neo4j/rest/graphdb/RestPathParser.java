package org.neo4j.rest.graphdb;

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
