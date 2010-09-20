package org.springframework.datastore.graph.neo4j.spi.relationship;

import java.lang.reflect.Constructor;

import org.neo4j.graphdb.Relationship;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.persistence.support.EntityInstantiator;

import sun.reflect.ReflectionFactory;

public class ConstructorBypassingGraphRelationshipInstantiator implements EntityInstantiator<RelationshipBacked, Relationship> {
	
	protected static <T> T createWithoutConstructorInvocation(Class<T> clazz) {
		return createWithoutConstructorInvocation(clazz, Object.class);
	}

	@SuppressWarnings("unchecked")
	protected static <T> T createWithoutConstructorInvocation(Class<T> clazz, Class<? super T> parent) {
		try {
			ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
			Constructor<?> objDef = parent.getDeclaredConstructor();
			Constructor<?> intConstr = rf.newConstructorForSerialization(clazz,
					objDef);
			return clazz.cast(intConstr.newInstance());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot create object", e);
		}
	}
	
	@Override
	public <T extends RelationshipBacked> T createEntityFromState(Relationship r, Class<T> c) {
		T t = createWithoutConstructorInvocation(c);
		t.setUnderlyingState(r);
		return t; 
	}

}
