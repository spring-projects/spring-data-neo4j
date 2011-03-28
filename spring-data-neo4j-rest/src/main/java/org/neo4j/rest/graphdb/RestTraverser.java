package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.IterableWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 03.02.11
 */
public class RestTraverser implements Traverser {
    private final Collection<Path> paths;
    public RestTraverser(Collection col, RestGraphDatabase restGraphDatabase) {
        this.paths = parseToPaths(col,restGraphDatabase);
    }

    private Collection<Path> parseToPaths(Collection col, RestGraphDatabase restGraphDatabase) {
        Collection<Path> result=new ArrayList<Path>(col.size());
        for (Object path : col) {
            if (!(path instanceof Map)) throw new RuntimeException("Expected Map for Path representation but got: "+(path!=null ? path.getClass() : null));
            result.add(RestPathParser.parse((Map) path, restGraphDatabase));
        }
        return result;
    }

    public Iterable<Node> nodes() {
        return new IterableWrapper<Node, Path>(paths) {
            @Override
            protected Node underlyingObjectToObject(Path path) {
                return path.endNode();
            }
        };
    }

    public Iterable<Relationship> relationships() {
        return new IterableWrapper<Relationship, Path>(paths) {
            @Override
            protected Relationship underlyingObjectToObject(Path path) {
                return path.lastRelationship();
            }
        };
    }

    public Iterator<Path> iterator() {
        return paths.iterator();
    }
}
