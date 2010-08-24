package org.springframework.persistence.graph.neo4j;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class ManagedFieldAccessorSet<T> extends AbstractSet<T> {
	private final NodeBacked entity;
	final Set<T> delegate;
	private final FieldAccessor relationshipInfo;

	public ManagedFieldAccessorSet(NodeBacked entity, Object newVal, FieldAccessor relationshipInfo) {
		this.entity = entity;
		this.relationshipInfo = relationshipInfo;
		delegate = (Set<T>) newVal;
	}

	@Override
	public Iterator<T> iterator() {
		return delegate.iterator();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean add(T e) {
		boolean res = delegate.add(e);
		if (res) {
			relationshipInfo.apply(entity, delegate);
		}
		return res;
	}

	@Override
	public boolean remove(Object o) {
		boolean res = delegate.remove(o);
		if (res) {
			relationshipInfo.apply(entity, delegate);
		}
		return res;
	}
	
}