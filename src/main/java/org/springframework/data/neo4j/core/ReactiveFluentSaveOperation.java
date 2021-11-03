/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.apiguardian.api.API;

/**
 * {@link ReactiveFluentSaveOperation} allows creation and execution of Neo4j save operations in a fluent API style. It
 * is designed to be used together with the {@link FluentFindOperation fluent find operations}.
 * <p>
 * Both interfaces provide a way to specify a pair of two types: A domain type and a result (projected) type.
 * The fluent save operations are mainly used with DTO based projections. Closed interface projections won't be that
 * helpful when you received them via {@link FluentFindOperation fluent find operations} as they won't be modifiable.
 *
 * @author Michael J. Simons
 * @since 6.2
 */
@API(status = API.Status.STABLE, since = "6.2")
public interface ReactiveFluentSaveOperation {

	/**
	 * Start creating a save operation for the given {@literal domainType}.
	 *
	 * @param domainType must not be {@literal null}.
	 * @return new instance of {@link ExecutableSave}.
	 * @throws IllegalArgumentException if domainType is {@literal null}.
	 */
	<T> ExecutableSave<T> save(Class<T> domainType);

	/**
	 * After the domain type has been specified, related projections or instances of the domain type can be saved.
	 *
	 * @param <DT> the domain type
	 */
	interface ExecutableSave<DT> {

		/**
		 * @param instance The instance to be saved
		 * @param <T>      The type of the instance passed to this method. It should be the same as the domain type before
		 *                 or a projection of the domain type. If they are not related, the results may be undefined.
		 * @return The saved instance, can also be a new object, so you are recommended to use this instance after
		 * the save operation
		 */
		<T> Mono<T> one(T instance);

		/**
		 * @param instances The instances to be saved
		 * @param <T>       The type of the instances passed to this method. It should be the same as the domain type before
		 *                  or a projection of the domain type. If they are not related, the results may be undefined.
		 * @return The saved instances, can also be a new objects, so you are recommended to use those instances
		 * after the save operation
		 */
		<T> Flux<T> all(Iterable<T> instances);
	}
}
