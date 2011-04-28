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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.core.EntityState;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.DoReturn;

import java.lang.reflect.Field;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * @param <T>
 */
public class ManagedFieldAccessorSet<ENTITY,T> extends AbstractSet<T> {
	private final ENTITY entity;
	final Set<T> delegate;
	private final Field field;

	public ManagedFieldAccessorSet(final ENTITY entity, final Object newVal, final Field field) {
		this.entity = entity;
		this.field = field;
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
        if (entity instanceof NodeBacked) {
            NodeBacked nodeBacked = (NodeBacked) entity;
            final EntityState<NodeBacked,Node> entityState = nodeBacked.getEntityState();
            updateValue(entityState);
        }
        if (entity instanceof RelationshipBacked) {
            RelationshipBacked relationshipBacked = (RelationshipBacked) entity;
            updateValue(relationshipBacked.getEntityState());
        }
    }

    private Object updateValue(EntityState entityState) {
        try {
            final Object newValue = entityState.setValue(field, delegate);
            if (newValue instanceof DoReturn) return DoReturn.unwrap(newValue);
            field.setAccessible(true);
            field.set(entity,newValue);
            return newValue;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not update field "+field+" to new value of type "+delegate.getClass());
        }
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

    @Override
    public boolean removeAll(Collection<?> c) {
        if (delegate.removeAll(c)) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (delegate.remove(o)) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (delegate.retainAll(c)) {
            update();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        delegate.clear();
        update();
    }
}