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

package org.springframework.data.graph.neo4j.support.relationship;

import java.lang.reflect.Constructor;

import org.neo4j.graphdb.Relationship;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.persistence.EntityInstantiator;

import sun.reflect.ReflectionFactory;

/**
 * Instantiator for relationship entities uses non constructor invoking {@link ReflectionFactory} internal to sun reflect
 * package.
 * Part of the SPI, not intended for public use.
 */

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
		t.setPersistentState(r);
		return t; 
	}

}
