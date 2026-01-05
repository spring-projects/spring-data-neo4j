/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.aot;

import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.data.domain.ManagedTypes;

/**
 * The set of types managed by Neo4j.
 *
 * @author Gerrit Meier
 * @since 7.0.0
 */
public final class Neo4jManagedTypes implements ManagedTypes {

	private final ManagedTypes delegate;

	private Neo4jManagedTypes(ManagedTypes types) {
		this.delegate = types;
	}

	/**
	 * Wraps an existing {@link ManagedTypes} object with {@link Neo4jManagedTypes}.
	 * @param managedTypes existing types to be wrapped
	 * @return new instance of {@link Neo4jManagedTypes} initialized from an existing set
	 * of managed types
	 */
	public static Neo4jManagedTypes from(ManagedTypes managedTypes) {
		return new Neo4jManagedTypes(managedTypes);
	}

	/**
	 * Factory method used to construct {@link Neo4jManagedTypes} from the given array of
	 * {@link Class types}.
	 * @param types array of {@link Class types} used to initialize the
	 * {@link ManagedTypes}; must not be {@literal null}
	 * @return new instance of {@link Neo4jManagedTypes} initialized from {@link Class
	 * types}
	 */
	public static Neo4jManagedTypes from(Class<?>... types) {
		return fromIterable(Arrays.asList(types));
	}

	/**
	 * Factory method used to construct {@link Neo4jManagedTypes} from the given, required
	 * {@link Iterable} of {@link Class types}.
	 * @param types {@link Iterable} of {@link Class types} used to initialize the
	 * {@link ManagedTypes}; must not be {@literal null}.
	 * @return new instance of {@link Neo4jManagedTypes} initialized the given, required
	 * {@link Iterable} of {@link Class types}.
	 */
	public static Neo4jManagedTypes fromIterable(Iterable<? extends Class<?>> types) {
		return from(ManagedTypes.fromIterable(types));
	}

	/**
	 * Factory method to return an empty {@link Neo4jManagedTypes} object.
	 * @return an empty {@link Neo4jManagedTypes} object.
	 */
	public static Neo4jManagedTypes empty() {
		return from(ManagedTypes.empty());
	}

	@Override
	public void forEach(Consumer<Class<?>> action) {
		this.delegate.forEach(action);
	}

}
