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
package org.springframework.data.neo4j.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.MappingSupport;
import org.springframework.data.neo4j.core.mapping.NoRootNodeMappingException;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;

/**
 * Typed preparation of a query that is used to create either an executable query.
 * Executable queries come in two fashions: imperative and reactive. Depending on which
 * client is used to retrieve one, you get one or the other.
 * <p>
 * When no mapping function is provided, the Neo4j client will assume a simple type to be
 * returned. Otherwise, make sure that the query fits to the mapping function, that is: It
 * must return all nodes, relationships and paths that is expected by the mapping function
 * to work correctly.
 *
 * @param <T> the type of the objects returned by this query.
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class PreparedQuery<T> {

	private final Class<T> resultType;

	private final QueryFragmentsAndParameters queryFragmentsAndParameters;

	@Nullable
	private final Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunctionSupplier;

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private volatile Optional<BiFunction<TypeSystem, Record, T>> lastMappingFunction = Optional.empty();

	private PreparedQuery(OptionalBuildSteps<T> optionalBuildSteps) {
		this.resultType = optionalBuildSteps.resultType;
		this.mappingFunctionSupplier = optionalBuildSteps.mappingFunctionSupplier;
		this.queryFragmentsAndParameters = optionalBuildSteps.queryFragmentsAndParameters;
	}

	public static <CT> RequiredBuildStep<CT> queryFor(Class<CT> resultType) {
		return new RequiredBuildStep<>(resultType);
	}

	public Class<T> getResultType() {
		return this.resultType;
	}

	@SuppressWarnings("unchecked")
	public synchronized Optional<BiFunction<TypeSystem, Record, T>> getOptionalMappingFunction() {
		this.lastMappingFunction = Optional.ofNullable(this.mappingFunctionSupplier)
			.map(Supplier::get)
			.map(f -> (BiFunction<TypeSystem, Record, T>) new AggregatingMappingFunction(f));
		return this.lastMappingFunction;
	}

	synchronized boolean resultsHaveBeenAggregated() {
		return this.lastMappingFunction.filter(AggregatingMappingFunction.class::isInstance)
			.map(AggregatingMappingFunction.class::cast)
			.map(AggregatingMappingFunction::hasAggregated)
			.orElse(false);
	}

	public QueryFragmentsAndParameters getQueryFragmentsAndParameters() {
		return this.queryFragmentsAndParameters;
	}

	/**
	 * Step configuring the query to be used.
	 *
	 * @param <CT> the concrete type of this build step.
	 * @since 6.0
	 */
	public static final class RequiredBuildStep<CT> {

		private final Class<CT> resultType;

		private RequiredBuildStep(Class<CT> resultType) {
			this.resultType = resultType;
		}

		public OptionalBuildSteps<CT> withCypherQuery(String cypherQuery) {
			return new OptionalBuildSteps<>(this.resultType, new QueryFragmentsAndParameters(cypherQuery));
		}

		public OptionalBuildSteps<CT> withQueryFragmentsAndParameters(
				QueryFragmentsAndParameters queryFragmentsAndParameters) {
			return new OptionalBuildSteps<>(this.resultType, queryFragmentsAndParameters);
		}

	}

	/**
	 * Step configuring parameters or mapping functions.
	 *
	 * @param <CT> the concrete type of this build step.
	 * @since 6.0
	 */
	public static final class OptionalBuildSteps<CT> {

		final Class<CT> resultType;

		final QueryFragmentsAndParameters queryFragmentsAndParameters;

		@Nullable
		Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunctionSupplier;

		OptionalBuildSteps(Class<CT> resultType, QueryFragmentsAndParameters queryFragmentsAndParameters) {
			this.resultType = resultType;
			this.queryFragmentsAndParameters = queryFragmentsAndParameters;
		}

		/**
		 * This replaces the current parameters.
		 * @param newParameters the new parameters for the prepared query.
		 * @return this builder
		 */
		public OptionalBuildSteps<CT> withParameters(@Nullable Map<String, Object> newParameters) {
			this.queryFragmentsAndParameters.setParameters(Objects.requireNonNullElseGet(newParameters, Map::of));
			return this;
		}

		public OptionalBuildSteps<CT> usingMappingFunction(
				@Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> newMappingFunction) {
			this.mappingFunctionSupplier = newMappingFunction;
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
				return new LinkedHashSet<Object>(aggregatePath(t, value, Collections.emptyList()));
			}
			return value.asList(v -> this.target.apply(t, v));
		}

		private Collection<?> aggregatePath(TypeSystem t, Value value,
				List<Map.Entry<String, Value>> additionalValues) {

			// We are using linked hash sets here so that the order of nodes will be
			// stable and match that of the path.
			Set<Object> result = new LinkedHashSet<>();
			Set<Value> nodes = new LinkedHashSet<>();
			Set<Value> relationships = new LinkedHashSet<>();

			List<Path> paths = value.hasType(t.PATH()) ? Collections.singletonList(value.asPath())
					: value.asList(Value::asPath);

			for (Path path : paths) {
				if (path.length() == 0) {
					// should only be exactly one
					Iterable<Node> pathNodes = path.nodes();
					for (Node pathNode : pathNodes) {
						nodes.add(Values.value(pathNode));
					}
					continue;
				}
				Node lastNode = null;
				for (Path.Segment segment : path) {
					Node start = segment.start();
					if (start != null) {
						nodes.add(Values.value(start));
					}
					lastNode = segment.end();
					relationships.add(Values.value(segment.relationship()));
				}
				if (lastNode != null) {
					nodes.add(Values.value(lastNode));
				}
			}

			// This loop synthesizes a node, it's relationship and all related nodes for
			// all nodes in a path.
			// All other nodes must be assumed to somehow related
			Map<String, Value> mapValue = new HashMap<>();
			// Those values and the combinations with the relationships will stay constant
			// for each node in question
			additionalValues.forEach(e -> mapValue.put(e.getKey(), e.getValue()));
			mapValue.put(Constants.NAME_OF_SYNTHESIZED_RELATIONS, Values.value(relationships));
			mapValue.put(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES, Values.value(nodes));

			for (Value rootNode : nodes) {
				mapValue.put(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE, rootNode);
				try {
					result.add(this.target.apply(t, Values.value(mapValue)));
				}
				catch (NoRootNodeMappingException ex) {
					// This is the case for nodes on the path that are not of the target
					// type
					// We can safely ignore those.
				}
			}

			return result;
		}

		@Override
		// Suppressing the warnings for accessing `pathValues`: `partitioningBy`
		// will always provide entries for `true` and `false`
		@SuppressWarnings("NullAway")
		public Object apply(TypeSystem t, Record r) {

			if (r.size() == 1) {
				Value value = r.get(0);
				if (value.hasType(t.LIST())) {
					this.aggregated.compareAndSet(false, true);
					return aggregateList(t, value);
				}
				else if (value.hasType(t.PATH())) {
					this.aggregated.compareAndSet(false, true);
					return aggregatePath(t, value, Collections.emptyList());
				}
			}

			try {
				return this.target.apply(t, r);
			}
			catch (NoRootNodeMappingException ex) {

				// We didn't find anything on the top level. It still can be a path plus
				// some additional information
				// to enrich the nodes on the path with.
				Map<Boolean, List<Map.Entry<String, Value>>> pathValues = r.asMap(Function.identity())
					.entrySet()
					.stream()
					.collect(Collectors.partitioningBy(entry -> entry.getValue().hasType(t.PATH())));
				if (pathValues.get(true).size() == 1) {
					this.aggregated.compareAndSet(false, true);
					return aggregatePath(t, pathValues.get(true).get(0).getValue(), pathValues.get(false));
				}
				throw ex;
			}
		}

		boolean hasAggregated() {
			return this.aggregated.get();
		}

	}

}
