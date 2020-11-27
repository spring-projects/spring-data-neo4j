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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.MappingSupport;
import org.springframework.data.neo4j.core.mapping.NoRootNodeMappingException;
import org.springframework.lang.Nullable;

/**
 * Typed preparation of a query that is used to create either an executable query. Executable queries come in two
 * fashions: imperative and reactive. Depending on which client is used to retrieve one, you get one or the other.
 * <p>
 * When no mapping function is provided, the Neo4j client will assume a simple type to be returned. Otherwise make sure
 * that the query fits to the mapping function, that is: It must return all nodes, relationships and paths that is
 * expected by the mapping function to work correctly.
 *
 * @param <T> The type of the objects returned by this query.
 * @author Michael J. Simons
 * @soundtrack Deichkind - Arbeit nervt
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
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
		if (optionalBuildSteps.mappingFunction == null) {
			this.mappingFunction = null;
		} else {
			this.mappingFunction = (BiFunction<TypeSystem, Record, T>) new AggregatingMappingFunction(
					optionalBuildSteps.mappingFunction);
		}
		this.cypherQuery = optionalBuildSteps.cypherQuery;
		this.parameters = optionalBuildSteps.parameters;
	}

	public Class<T> getResultType() {
		return this.resultType;
	}

	public Optional<BiFunction<TypeSystem, Record, T>> getOptionalMappingFunction() {
		return Optional.ofNullable(mappingFunction);
	}

	boolean resultsHaveBeenAggregated() {
		return this.mappingFunction != null && ((AggregatingMappingFunction) this.mappingFunction).hasAggregated();
	}

	public String getCypherQuery() {
		return this.cypherQuery;
	}

	public Map<String, Object> getParameters() {
		return this.parameters;
	}

	/**
	 * @param <CT> The concrete type of this build step.
	 * @since 6.0
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
	 * @since 6.0
	 */
	public static class OptionalBuildSteps<CT> {

		final Class<CT> resultType;
		final String cypherQuery;
		Map<String, Object> parameters = Collections.emptyMap();
		@Nullable BiFunction<TypeSystem, MapAccessor, ?> mappingFunction;

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
				@Nullable BiFunction<TypeSystem, MapAccessor, ?> newMappingFunction) {
			this.mappingFunction = newMappingFunction;
			return this;
		}

		public PreparedQuery<CT> build() {
			return new PreparedQuery<>(this);
		}
	}

	private static class AggregatingMappingFunction implements BiFunction<TypeSystem, Record, Object> {

		private final BiFunction<TypeSystem, MapAccessor, ?> target;
		private final AtomicBoolean aggregated = new AtomicBoolean(false);

		AggregatingMappingFunction(BiFunction<TypeSystem, MapAccessor, ?> target) {
			this.target = target;
		}

		private Collection<?> aggregateList(TypeSystem t, Value value) {

			if (MappingSupport.isListContainingOnly(t.LIST(), t.PATH()).test(value)) {
				Set<Object> result = new LinkedHashSet<>();
				for (Value path : value.values()) {
					result.addAll(aggregatePath(t, path, Collections.emptyList()));
				}
				return result;
			}
			return value.asList(v -> target.apply(t, v));
		}

		private Collection<?> aggregatePath(TypeSystem t, Value value,
				List<Map.Entry<String, Value>> additionalValues) {
			Path path = value.asPath();

			// We are using a linked hash set here so that the order of nodes will be stable and
			// match the one the path
			Set<Object> result = new LinkedHashSet<>();
			path.iterator().forEachRemaining(segment -> {

				Map<String, Value> mapValue = new HashMap<>();
				mapValue.put(Constants.NAME_OF_IS_PATH_SEGMENT, Values.value(true));
				mapValue.put(Constants.PATH_START, Values.value(segment.start()));
				mapValue.put(Constants.PATH_RELATIONSHIP, Values.value(segment.relationship()));
				mapValue.put(Constants.PATH_END, Values.value(segment.end()));
				additionalValues.forEach(e -> mapValue.put(e.getKey(), e.getValue()));

				Value v = Values.value(mapValue);
				try {
					result.add(target.apply(t, v));
				} catch (NoRootNodeMappingException e) {
					// This is the case for nodes on the path that are not of the target type
					// We can safely ignore those.
				}
			});

			return result;
		}

		@Override
		public Object apply(TypeSystem t, Record r) {

			if (r.size() == 1) {
				Value value = r.get(0);
				if (value.hasType(t.LIST())) {
					aggregated.compareAndSet(false, true);
					return aggregateList(t, value);
				} else if (value.hasType(t.PATH())) {
					aggregated.compareAndSet(false, true);
					return aggregatePath(t, value, Collections.emptyList());
				}
			}

			try {
				return target.apply(t, new RecordMapAccessor(r));
			} catch (NoRootNodeMappingException e) {

				// We didn't find anything on the top level. It still can be a path plus some additional information
				// to enrich the nodes on the path with.
				Map<Boolean, List<Map.Entry<String, Value>>> pathValues = r.asMap(Function.identity()).entrySet()
						.stream()
						.collect(Collectors.partitioningBy(entry -> entry.getValue().hasType(t.PATH())));
				if (pathValues.get(true).size() == 1) {
					aggregated.compareAndSet(false, true);
					return aggregatePath(t, pathValues.get(true).get(0).getValue(), pathValues.get(false));
				}
				throw e;
			}
		}

		boolean hasAggregated() {
			return aggregated.get();
		}
	}
}
