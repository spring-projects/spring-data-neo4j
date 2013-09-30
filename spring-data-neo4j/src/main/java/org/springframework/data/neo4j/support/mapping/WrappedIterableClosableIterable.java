package org.springframework.data.neo4j.support.mapping;

import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.ClosableIterable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class WrappedIterableClosableIterable<S> implements ClosableIterable {

    private Iterable<S> iterable;
    private boolean canBeClosed;
    private List<ResourceIterator> openedResourceIterators;
    public WrappedIterableClosableIterable(Iterable<S> iterable) {
        this.iterable = iterable;
        this.canBeClosed = (iterable instanceof ResourceIterable || iterable instanceof ClosableIterable);
        this.openedResourceIterators = new ArrayList<ResourceIterator>();
    }

    /**
     * Note: Once this method has been called, it is not valid to
     * call any other methods on this object thereafter;
     */
    @Override
    public void close() {
        if (canBeClosed) {
            if (iterable instanceof ClosableIterable) {
              ((ClosableIterable)iterable).close();
            }
            for (ResourceIterator ri : openedResourceIterators) {
              ri.close();
            }
        }
        openedResourceIterators = null;
        iterable = null;
    }

    @Override
    public Iterator iterator() {
        Iterator it = iterable.iterator();
        if (it instanceof ResourceIterator) {
            openedResourceIterators.add((ResourceIterator)it);
        }
        return it;
    }
};
