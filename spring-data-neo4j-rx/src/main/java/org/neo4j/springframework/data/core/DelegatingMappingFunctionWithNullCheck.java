/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core;

import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;

/**
 * A delegating mapping function that first calls the delegate to get a record map and than checks the returned
 * value for {@literal null} and in the case of a null value, an {@link IllegalStateException} will be thrown.
 * <p>
 * This class has been introduced instead of {@code Function#andThen} notion to be able throw a decent exception
 * containing some information about the delegate used and which record was problematic.
 *
 * @author Michael J. Simons
 * @param <T> The expected type of this function
 * @soundtrack Manowar - Fighting The World
 * @since 1.0
 */
class DelegatingMappingFunctionWithNullCheck<T> implements BiFunction<TypeSystem, Record, T> {

	BiFunction<TypeSystem, Record, T> delegate;

	DelegatingMappingFunctionWithNullCheck(BiFunction<TypeSystem, Record, T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public T apply(TypeSystem typeSystem, Record record) {
		T t = delegate.apply(typeSystem, record);
		if (t == null) {
			throw new IllegalStateException(
				"Mapping function " + delegate + " returned illegal null value for record " + record);
		}
		return t;
	}
}
