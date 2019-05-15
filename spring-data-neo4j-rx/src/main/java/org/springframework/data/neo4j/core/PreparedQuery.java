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
package org.springframework.data.neo4j.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apiguardian.api.API;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.lang.Nullable;

/**
 * @param <T> The type of the objects returned by this query.
 * @author Michael J. Simons
 * @soundtrack Deichkind - Arbeit nervt
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface PreparedQuery<T> {

	static <CT> RequiredBuildStep<CT> queryFor(Class<CT> resultType) {
		return new RequiredBuildStep<CT>(resultType);
	}

	Class<T> getResultType();

	Optional<BiFunction<TypeSystem, Record, T>> getOptionalMappingFunction();

	String getCypherQuery();

	Map<String, Object> getParameters();

	/**
	 * @param <CT> The concrete type of this build step.
	 */
	class RequiredBuildStep<CT> {
		private final Class<CT> resultType;

		private RequiredBuildStep(Class<CT> resultType) {
			this.resultType = resultType;
		}

		public OptionalBuildSteps<CT> withCypherQuery(String cypherQuery) {
			return new OptionalBuildSteps<>(resultType, cypherQuery);
		}
	}

	/**
	 * @param <CT> The concrete type of this build step.
	 */
	class OptionalBuildSteps<CT> {

		final Class<CT> resultType;
		final String cypherQuery;
		Map<String, Object> parameters = Collections.emptyMap();
		@Nullable BiFunction<TypeSystem, Record, ?> mappingFunction;

		OptionalBuildSteps(Class<CT> resultType, String cypherQuery) {
			this.resultType = resultType;
			this.cypherQuery = cypherQuery;
		}

		/**
		 * This replaces the current parameters.
		 *
		 * @param newParameters The new parameters for the prepared query.
		 * @return This builder.
		 */
		public OptionalBuildSteps<CT> withParameters(Map<String, Object> newParameters) {
			this.parameters = new HashMap<>(newParameters);
			return this;
		}

		public OptionalBuildSteps<CT> usingMappingFunction(
			@Nullable BiFunction<TypeSystem, Record, ?> newMappingFunction) {
			this.mappingFunction = newMappingFunction;
			return this;
		}

		public PreparedQuery<CT> build() {
			return new DefaultPreparedQuery<>(this);
		}
	}
}
