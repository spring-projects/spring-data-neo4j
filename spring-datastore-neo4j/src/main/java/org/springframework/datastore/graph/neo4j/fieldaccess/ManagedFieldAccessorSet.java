package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.datastore.graph.api.NodeBacked;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * TODO handle all mutating methods
 * @param <T>
 */
public class ManagedFieldAccessorSet<ENTITY,T> extends AbstractSet<T> {
	private final ENTITY entity;
	final Set<T> delegate;
	private final FieldAccessor<ENTITY,T> fieldAccessor;

	public ManagedFieldAccessorSet(final ENTITY entity, final Object newVal, final FieldAccessor fieldAccessor) {
		this.entity = entity;
		this.fieldAccessor = fieldAccessor;
		delegate = (Set<T>) newVal;
	}

	@Override
	public Iterator<T> iterator() {
        final Iterator<T> iterator = delegate.iterator();
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
                update();
            }
        };
	}

    private void update() {
        fieldAccessor.setValue(entity, delegate);
    }

    @Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean add(final T e) {
		final boolean res = delegate.add(e);
		if (res) update();
		return res;
	}
}