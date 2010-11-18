/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.springframework.data.graph.api.NodeBacked;

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