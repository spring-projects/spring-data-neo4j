package org.springframework.datastore.graph.neo4j.finder;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.index.IndexHits;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.util.Collections;

public class Finder<T extends NodeBacked> {

    private final Class<T> clazz;
    private final GraphDatabaseContext graphDatabaseContext;

    public Finder(final Class<T> clazz, final GraphDatabaseContext graphDatabaseContext) {
        this.clazz = clazz;
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public long count() {
        return graphDatabaseContext.count(clazz);
    }

    public Iterable<T> findAll() {
        return graphDatabaseContext.findAll(clazz);
    }

    public T findById(final long id) {
        try {
            return graphDatabaseContext.createEntityFromState(graphDatabaseContext.getNodeById(id), clazz);
        } catch (NotFoundException e) {
            return null;
        }
    }

    public T findByPropertyValue(final String property, final Object value) {
        try {
            final Node node = graphDatabaseContext.getSingleIndexedNode(property, value);
            if (node == null) return null;
            return graphDatabaseContext.createEntityFromState(node, clazz);
        } catch (NotFoundException e) {
            return null;
        }

    }

    public Iterable<T> findAllByPropertyValue(final String property, final Object value) {
        try {
            final IndexHits<Node> nodes = graphDatabaseContext.getIndexedNodes(property, value);
            if (nodes == null) return Collections.emptyList();
            return new IterableWrapper<T, Node>(nodes) {
                @Override
                protected T underlyingObjectToObject(final Node node) {
                    return graphDatabaseContext.createEntityFromState(node, clazz);
                }
            };
        } catch (NotFoundException e) {
            return null;
        }
    }

    public <N extends NodeBacked> Iterable<T> findAllByTraversal(final N startNode, final TraversalDescription traversalDescription) {
        return (Iterable<T>) startNode.find(clazz, traversalDescription);
    }
}

