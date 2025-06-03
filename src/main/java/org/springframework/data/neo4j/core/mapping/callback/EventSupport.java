/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.mapping.callback;

import org.apiguardian.api.API;
import org.neo4j.driver.types.MapAccessor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * Utility class that orchestrates {@link EntityCallbacks}. Not to be used outside the
 * framework.
 *
 * @author Michael J. Simons
 * @since 6.0.2
 */
@API(status = INTERNAL, since = "6.0.2")
public final class EventSupport {

	private final EntityCallbacks entityCallbacks;

	private EventSupport(EntityCallbacks entityCallbacks) {
		this.entityCallbacks = entityCallbacks;
	}

	/**
	 * Creates event support containing the required default events plus all entity
	 * callbacks discoverable through the {@link BeanFactory}.
	 * @param context the mapping context that is used in some of the callbacks
	 * @param beanFactory the bean factory used to discover additional callbacks
	 * @return a new instance of the event support
	 */
	public static EventSupport discoverCallbacks(Neo4jMappingContext context, BeanFactory beanFactory) {

		EntityCallbacks entityCallbacks = EntityCallbacks.create(beanFactory);
		addDefaultEntityCallbacks(context, entityCallbacks);
		return new EventSupport(entityCallbacks);
	}

	/**
	 * Creates event support containing the required default events plus all explicitly
	 * defined events.
	 * @param context the mapping context that is used in some of the callbacks
	 * @param entityCallbacks predefined callbacks.
	 * @return a new instance of the event support
	 */
	public static EventSupport useExistingCallbacks(Neo4jMappingContext context, EntityCallbacks entityCallbacks) {

		addDefaultEntityCallbacks(context, entityCallbacks);
		return new EventSupport(entityCallbacks);
	}

	private static void addDefaultEntityCallbacks(Neo4jMappingContext context, EntityCallbacks entityCallbacks) {

		entityCallbacks.addEntityCallback(new IdGeneratingBeforeBindCallback(context));
		entityCallbacks.addEntityCallback(new PostLoadInvocation(context));
	}

	public <T> T maybeCallBeforeBind(T object) {

		if (object == null) {
			return object;
		}
		return this.entityCallbacks.callback(BeforeBindCallback.class, object);
	}

	/**
	 * Will be called after any conversion.
	 * @param object the freshly converted instance
	 * @param entity the entity
	 * @param source the source of the instance
	 * @param <T> the expected type
	 * @return the instance to which the callback was applied to
	 */
	public <T> T maybeCallAfterConvert(T object, Neo4jPersistentEntity<T> entity, MapAccessor source) {

		if (object == null) {
			return object;
		}
		return this.entityCallbacks.callback(AfterConvertCallback.class, object, entity, source);
	}

}
