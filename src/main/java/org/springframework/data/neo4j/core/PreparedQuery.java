/*
 * Copyright 2011-2020 the original author or authors.
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
 * Typed preparation of a query that is used to create either an executable query.
 * Executable queries come in two fashions: imperative and reactive. Depending on which client is used to retrieve one,
 * you get one or the other.
 * <p>
 * When no mapping function is provided, the Neo4j client will assume a simple type to be returned. Otherwise make sure
 * that the query fits to the mapping function, that is: It must return all nodes, relationships and paths that is expected
 * by the mapping function to work correctly.
 *
 * @param <T> The type of the objects returned by this query.
 * @author Michael J. Simons
 * @soundtrack Deichkind - Arbeit nervt
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class PreparedQuery<T> {

	public static <CT> RequiredBuildStep<CT> queryFor(Class<CT> resultType) {
		return new RequiredBuildStep<CT>(resultType);
	}

	private final Class<T> resultType;
	private final String cypherQuery;
	private final Map<String, Object> parameters;
	private final @Nullable BiFunction<TypeSystem, Record, T> mappingFunction;

	private PreparedQuery(OptionalBuildSteps<T> optionalBuildSteps) {
		this.resultType = optionalBuildSteps.resultType;
		this.mappingFunction = (BiFunction<TypeSystem, Record, T>) optionalBuildSteps.mappingFunction;
		this.cypherQuery = optionalBuildSteps.cypherQuery;
		this.parameters = optionalBuildSteps.parameters;
	}

	public Class<T> getResultType() {
		return this.resultType;
	}

	public Optional<BiFunction<TypeSystem, Record, T>> getOptionalMappingFunction() {
		return Optional.ofNullable(mappingFunction);
	}

	public String getCypherQuery() {
		return this.cypherQuery;
	}

	public Map<String, Object> getParameters() {
		return this.parameters;
	}

	/**
	 * @param <CT> The concrete type of this build step.
	 * @since 1.0
	 */
	public static class RequiredBuildStep<CT> {
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
	 * @since 1.0
	 */
	public static class OptionalBuildSteps<CT> {

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
			return new PreparedQuery<>(this);
		}
	}
}
