package org.springframework.data.neo4j.support.mapping;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.ClosableIterable;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO - Sort out closing of Iterators properly
 *
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class ResourceIterableClosableIterable implements ClosableIterable , ResourceIterable {

    private ResourceIterable<Node> resourceIterable;
    private List<ResourceIterator> requestedIterators;

    public ResourceIterableClosableIterable(ResourceIterable<Node> resourceIterable) {
        this.resourceIterable = resourceIterable;
        this.requestedIterators = new ArrayList<ResourceIterator>();
    }

    @Override
    public void close() {
        for (ResourceIterator ri : requestedIterators) {
            ri.close();
        }
    }

    @Override
    public ResourceIterator iterator() {
        ResourceIterator ri = resourceIterable.iterator();
        requestedIterators.add(ri);
        return ri;
    }
};
