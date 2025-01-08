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
package org.springframework.data.neo4j.core.mapping;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.lang.Nullable;

/**
 * All property converters will be wrapped by this class. It adds the information if a converter needs to be applied to
 * a complete collection or to individual values.
 *
 * @author Michael J. Simons
 * @param <T> The type of the property this converter converts.
 * @soundtrack Roger Taylor - The Outsider
 */
final class NullSafeNeo4jPersistentPropertyConverter<T> implements Neo4jPersistentPropertyConverter<T> {

	/**
	 * The actual delegate doing the conversation.
	 */
	private final Neo4jPersistentPropertyConverter<T> delegate;

	/**
	 * {@literal false} for all non-composite converters. If true, {@literal null} will be passed to the writing converter
	 */
	private final boolean passNullOnWrite;

	/**
	 * {@literal true} if the converter needs to be applied to a whole collection.
	 */
	private final boolean forCollection;

	NullSafeNeo4jPersistentPropertyConverter(Neo4jPersistentPropertyConverter<T> delegate, boolean passNullOnWrite,
			boolean forCollection) {
		this.delegate = delegate;
		this.passNullOnWrite = passNullOnWrite;
		this.forCollection = forCollection;
	}

	@Override
	public Value write(@Nullable T source) {
		if (source == null) {
			return passNullOnWrite ? delegate.write(source) : Values.NULL;
		}
		return delegate.write(source);
	}

	@Override @Nullable
	public T read(@Nullable Value source) {
		return source == null || source.isNull() ? null : delegate.read(source);
	}

	public boolean isForCollection() {
		return forCollection;
	}
}
