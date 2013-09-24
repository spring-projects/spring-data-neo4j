package org.springframework.data.neo4j.support.mapping;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.ClosableIterable;

import java.util.Iterator;

/**
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class WrappedIterableClosableIterable<S> implements ClosableIterable {

    private Iterator iterator;
    public WrappedIterableClosableIterable(Iterable<S> iterable) {
        this.iterator = iterable.iterator();
    }

    @Override
    public void close() {
        // no op
        // Is this valid???
    }

    @Override
    public Iterator iterator() {
        return iterator;
    }
};
