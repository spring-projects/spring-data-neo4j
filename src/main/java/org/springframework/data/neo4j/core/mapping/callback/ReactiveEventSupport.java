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
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * Utility class that orchestrates {@link EntityCallbacks}. Not to be used outside the
 * framework.
 *
 * @author Michael J. Simons
 * @since 6.0.2
 */
@API(status = INTERNAL, since = "6.0.2")
public final class ReactiveEventSupport {

	private final ReactiveEntityCallbacks entityCallbacks;

	private ReactiveEventSupport(ReactiveEntityCallbacks entityCallbacks) {
		this.entityCallbacks = entityCallbacks;
	}

	/**
	 * Creates event support containing the required default events plus all entity
	 * callbacks discoverable through the {@link BeanFactory}.
	 * @param context the mapping context that is used in some of the callbacks.
	 * @param beanFactory the bean factory used to discover additional callbacks.
	 * @return a new instance of the event support
	 */
	public static ReactiveEventSupport discoverCallbacks(Neo4jMappingContext context, BeanFactory beanFactory) {

		ReactiveEntityCallbacks entityCallbacks = ReactiveEntityCallbacks.create(beanFactory);
		addDefaultEntityCallbacks(context, entityCallbacks);
		return new ReactiveEventSupport(entityCallbacks);
	}

	/**
	 * Creates event support containing the required default events plus all explicitly
	 * defined events.
	 * @param context the mapping context that is used in some of the callbacks.
	 * @param entityCallbacks predefined callbacks.
	 * @return a new instance of the event support
	 */
	public static ReactiveEventSupport useExistingCallbacks(Neo4jMappingContext context,
			ReactiveEntityCallbacks entityCallbacks) {

		addDefaultEntityCallbacks(context, entityCallbacks);
		return new ReactiveEventSupport(entityCallbacks);
	}

	private static void addDefaultEntityCallbacks(Neo4jMappingContext context,
			ReactiveEntityCallbacks entityCallbacks) {

		entityCallbacks.addEntityCallback(new ReactiveIdGeneratingBeforeBindCallback(context));
		entityCallbacks.addEntityCallback(new PostLoadInvocation(context));
	}

	public <T> Mono<T> maybeCallBeforeBind(T object) {

		if (object == null) {
			return Mono.empty();
		}
		return this.entityCallbacks.callback(ReactiveBeforeBindCallback.class, object);
	}

}
