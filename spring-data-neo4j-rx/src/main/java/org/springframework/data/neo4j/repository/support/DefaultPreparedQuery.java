/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.repository.support;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.lang.Nullable;

/**
 * Typed preparation of a query that is used to create an {@link ExecutableQuery} of the same type.
 * <p/>
 * When no mapping function is provided, the Neo4j client will assume a simple type to be returned. Otherwise make sure
 * that the query fits to the mapping function, that is: It must return all nodes, relationships and paths that is expected
 * by the mapping function to work correctly.
 *
 * @param <T> The type of the objects returned by this query.
 * @author Michael J. Simons
 * @soundtrack Deichkind - Noch fünf Minuten Mutti
 * @since 1.0
 */
final class DefaultPreparedQuery<T> implements PreparedQuery<T> {

	private final Class<T> resultType;
	private final String cypherQuery;
	private final Map<String, Object> parameters;
	private final @Nullable BiFunction<TypeSystem, Record, T> mappingFunction;

	DefaultPreparedQuery(OptionalBuildSteps<T> optionalBuildSteps) {
		this.resultType = optionalBuildSteps.resultType;
		this.mappingFunction = (BiFunction<TypeSystem, Record, T>) optionalBuildSteps.mappingFunction;
		this.cypherQuery = optionalBuildSteps.cypherQuery;
		this.parameters = optionalBuildSteps.parameters;
	}

	@Override
	public Class<T> getResultType() {
		return this.resultType;
	}

	@Override
	public Optional<BiFunction<TypeSystem, Record, T>> getOptionalMappingFunction() {
		return Optional.ofNullable(mappingFunction);
	}

	@Override
	public String getCypherQuery() {
		return this.cypherQuery;
	}

	@Override
	public Map<String, Object> getParameters() {
		return this.parameters;
	}
}
