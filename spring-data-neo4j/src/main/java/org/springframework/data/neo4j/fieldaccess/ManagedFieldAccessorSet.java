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

import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.ManagedEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * This class provides a mechanism for managing and controlling access to
 * a Set based field on a SDN managed entity. The associated field typically
 * serves as a container for all the references to some other SDN entity(s).
 *
 * @param <T>
 */
public class ManagedFieldAccessorSet<T> extends AbstractSet<T> implements Serializable {

    private static final long serialVersionUID = 1L;

	private final Object entity;
	final Set<T> delegate;
	private final transient Neo4jPersistentProperty property;
    private final transient Neo4jTemplate ctx;
    private final transient FieldAccessor fieldAccessor;
    private final MappingPolicy mappingPolicy;

    @SuppressWarnings("unchecked")
    public ManagedFieldAccessorSet(final Object entity, final Object newVal, final Neo4jPersistentProperty property, Neo4jTemplate ctx, FieldAccessor fieldAccessor, final MappingPolicy mappingPolicy) {
		this.entity = entity;
		this.property = property;
        this.ctx = ctx;
        this.fieldAccessor = fieldAccessor;
        delegate = (Set<T>) newVal;
        this.mappingPolicy = mappingPolicy;
    }

    public static <T> ManagedFieldAccessorSet<T> create(Object entity, Set<T> result, MappingPolicy mappingPolicy, final Neo4jPersistentProperty property, final Neo4jTemplate template, final FieldAccessor fieldAccessor) {
        return new ManagedFieldAccessorSet<T>(entity, result, property, template, fieldAccessor, mappingPolicy);
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
        if (ctx.isManaged(entity)) {
            updateValueWithState(((ManagedEntity)entity).getEntityState());
        } else {
            updateValue();
        }
    }

    private Object updateValueWithState(EntityState entityState) {
        final Object newValue = entityState.setValue(property, delegate, mappingPolicy);
        if (newValue instanceof DoReturn) return DoReturn.unwrap(newValue);
        property.setValue(entity, newValue);
        return newValue;
    }

    private Object updateValue() {
        final Object newValue = fieldAccessor.setValue(entity,delegate, mappingPolicy);
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