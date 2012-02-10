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
package org.springframework.data.neo4j.support.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.mapping.EntityInstantiator;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.persistence.StateBackedCreator;
import org.springframework.data.persistence.StateProvider;
import org.springframework.util.ClassUtils;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Try for a constructor taking state: failing that, try a no-arg constructor and then setUnderlyingNode().
 * 
 * @author Rod Johnson
 */
public abstract class AbstractConstructorEntityInstantiator<STATE> implements EntityInstantiator<STATE> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<Class<?>, StateBackedCreator<?, STATE>> cache = new HashMap<Class<?>, StateBackedCreator<?, STATE>>();

	@SuppressWarnings("unchecked")
    public <T> T createEntityFromState(STATE n, Class<T> c, final MappingPolicy mappingPolicy) {
		try {
			StateBackedCreator<T, STATE> creator = (StateBackedCreator<T, STATE>) cache.get(c);
			if (creator != null)
				return creator.create(n, c);
			synchronized (cache) {
				creator = (StateBackedCreator<T, STATE>) cache.get(c);
				if (creator != null)
					return creator.create(n, c);
				Class<STATE> stateClass = (Class<STATE>) n.getClass();
				creator = createInstantiator(c, stateClass);
				cache.put(c, creator);
				return creator.create(n, c);
			}
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e.getTargetException());
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void setInstantiators(
			Map<Class<?>, StateBackedCreator<?, STATE>> instantiators) {
		this.cache.putAll(instantiators);
	}

	protected <T> StateBackedCreator<T, STATE> createInstantiator(Class<T> type,
			final Class<STATE> stateType) {
		StateBackedCreator<T, STATE> creator = stateTakingConstructorInstantiator(type, stateType);
		if (creator != null)
			return creator;
		creator = emptyConstructorStateSettingInstantiator(type, stateType);
		if (creator != null)
			return creator;
		return createFailingInstantiator(stateType);
	}

	protected <T> StateBackedCreator<T, STATE> createFailingInstantiator(
			final Class<STATE> stateType) {
		return new StateBackedCreator<T, STATE>() {
			public T create(STATE n, Class<T> c) throws Exception {
				throw new IllegalArgumentException(getFailingMessageForClass(c, stateType));
			}
		};
	}

	protected String getFailingMessageForClass(Class<?> entityClass, Class<STATE> stateClass) {
		return getClass().getSimpleName() + ": entity " + entityClass + " must have either a constructor taking ["
				+ stateClass + "] or a no-arg constructor and state setter.";
	}

	private <T> StateBackedCreator<T, STATE> emptyConstructorStateSettingInstantiator(
			Class<T> type, Class<STATE> stateType) {
		final Constructor<T> constructor = getNoArgConstructor(type);
		if (constructor == null)
			return null;

		log.info("Using " + type + " no-arg constructor");

		return new StateBackedCreator<T, STATE>() {
			public T create(STATE state, Class<T> c) throws Exception {
				try {
					StateProvider.setUnderlyingState(state);
					T newInstance = constructor.newInstance();
					setState(newInstance, state);
					return newInstance;
				} finally {
					StateProvider.retrieveState();
				}
			}
		};
	}

	protected <T> StateBackedCreator<T, STATE> createWithoutConstructorInvocation(
			final Class<T> type, Class<STATE> stateType) {
		ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
		Constructor<?> objectConstructor = getDeclaredConstructor(Object.class);
		final Constructor<?> serializationConstructor = rf.newConstructorForSerialization(type, objectConstructor);
		return new StateBackedCreator<T, STATE>() {
			public T create(STATE state, Class<T> c) throws Exception {
				final T result = type.cast(serializationConstructor.newInstance());
				setState(result, state);
				return result;
			}
		};
	}

	protected <T> Constructor<T> getNoArgConstructor(Class<T> type) {
		Constructor<T> constructor = ClassUtils.getConstructorIfAvailable(type);
		if (constructor != null)
			return constructor;
		return getDeclaredConstructor(type);
	}

	protected <T> StateBackedCreator<T, STATE> stateTakingConstructorInstantiator(
			Class<T> type, Class<STATE> stateType) {
		@SuppressWarnings("unchecked") Class<? extends STATE> stateInterface = (Class<? extends STATE>) ClassUtils.getAllInterfaces(stateType)[0];
		final Constructor<T> constructor = ClassUtils.getConstructorIfAvailable(type, stateInterface);
		if (constructor == null)
			return null;

		log.info("Using " + type + " constructor taking " + stateInterface);
		return new StateBackedCreator<T, STATE>() {
			public T create(STATE n, Class<T> c) throws Exception {
				return constructor.newInstance(n);
			}
		};
	}

	protected <T> Constructor<T> getDeclaredConstructor(Class<T> c) {
		try {
			final Constructor<T> declaredConstructor = c.getDeclaredConstructor();
			declaredConstructor.setAccessible(true);
			return declaredConstructor;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	/**
	 * Subclasses must implement to set state
	 * 
	 * @param entity
	 * @param s
	 */
	protected abstract void setState(Object entity, STATE s);

}
