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

package org.springframework.data.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.neo4j.core.EntityState;


import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * @param <T>
 */
public class ManagedFieldAccessorSet<T> extends AbstractSet<T> {
	private final Object entity;
	final Set<T> delegate;
	private final Neo4jPersistentProperty property;
    private final GraphDatabaseContext ctx;
    private final FieldAccessor fieldAccessor;

    public ManagedFieldAccessorSet(final Object entity, final Object newVal, final Neo4jPersistentProperty property, GraphDatabaseContext ctx, FieldAccessor fieldAccessor) {
		this.entity = entity;
		this.property = property;
        this.ctx = ctx;
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
        final Neo4jPersistentEntity<?> persistentEntity = property.getOwner();
        final PropertyContainer persistentState = persistentEntity.getPersistentState(entity, ctx);
        if (persistentEntity.isNodeEntity()) {
            updateValue();
        }
        if (persistentEntity.isRelationshipEntity()) {
            updateValue();
        }
    }

    private Object updateValue() {
        final Object newValue = fieldAccessor.setValue(entity,delegate);
        if (newValue instanceof DoReturn) return DoReturn.unwrap(newValue);
        property.setValue(entity, newValue);
        return newValue;
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