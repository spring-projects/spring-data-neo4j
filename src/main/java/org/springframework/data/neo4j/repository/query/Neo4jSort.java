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
package org.springframework.data.neo4j.repository.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;

/**
 * Layer around {@link Sort} to allow additional function calls within the sort
 * properties. Acts itself like {@link Sort} but does not provide its features.
 *
 * @author Gerrit Meier
 * @since 8.0.9
 */
public final class Neo4jSort extends Sort {

	/**
	 * List of Neo4j built-in functions.
	 */
	public static final Set<String> DEFAULT_ALLOWED_FUNCTIONS = Set.of("abs", "acos", "all", "allReduce",
			"allShortestPaths", "any", "asin", "atan", "atan2", "avg", "btrim", "cardinality", "ceil", "ceiling",
			"char_length", "character_length", "coalesce", "coll.distinct", "coll.flatten", "coll.indexOf",
			"coll.insert", "coll.max", "coll.min", "coll.remove", "coll.sort", "collect", "collect_list", "cos", "cosh",
			"cot", "coth", "count", "date", "date.realtime", "date.statement", "date.transaction", "date.truncate",
			"datetime", "datetime.fromepoch", "datetime.fromepochmillis", "datetime.realtime", "datetime.statement",
			"datetime.transaction", "datetime.truncate", "degrees", "duration", "duration.between", "duration.inDays",
			"duration.inMonths", "duration.inSeconds", "duration_between", "e", "elementId", "endNode", "exists", "exp",
			"file", "floor", "format", "graph.byElementId", "graph.byName", "haversin", "head", "id", "isEmpty",
			"isNaN", "keys", "labels", "last", "left", "length", "linenumber", "ln", "local_datetime", "local_time",
			"localdatetime", "localdatetime.realtime", "localdatetime.statement", "localdatetime.transaction",
			"localdatetime.truncate", "localtime", "localtime.realtime", "localtime.statement", "localtime.transaction",
			"localtime.truncate", "log", "log10", "lower", "ltrim", "max", "min", "nodes", "none", "normalize",
			"nullIf", "path_length", "percentileCont", "percentileDisc", "percentile_cont", "percentile_disc", "pi",
			"point", "point.distance", "point.withinBBox", "properties", "property_exists", "radians", "rand",
			"randomUUID", "range", "reduce", "relationships", "replace", "reverse", "right", "round", "rtrim",
			"shortestPath", "sign", "sin", "single", "sinh", "size", "split", "sqrt", "stDev", "stDevP", "startNode",
			"stdev_pop", "stdev_samp", "string.indexOf", "string.join", "string.regexReplace", "substring", "sum",
			"tail", "tan", "tanh", "time", "time.realtime", "time.statement", "time.transaction", "time.truncate",
			"timestamp", "toBoolean", "toBooleanList", "toBooleanOrNull", "toFloat", "toFloatList", "toFloatOrNull",
			"toInteger", "toIntegerList", "toIntegerOrNull", "toLower", "toString", "toStringList", "toStringOrNull",
			"toUpper", "trim", "type", "upper", "uuid", "uuid.leastSignificantBits", "uuid.mostSignificantBits",
			"valueType", "vector", "vector.similarity.cosine", "vector.similarity.euclidean", "vector_dimension_count",
			"vector_distance", "vector_norm", "zoned_datetime", "zoned_time");

	private final Sort wrapped;

	private final Set<String> additionalAllowedFunctions;

	protected Neo4jSort(Sort wrapped, Set<String> additionalAllowedFunctions) {
		super(wrapped.stream().toList());
		this.wrapped = wrapped;
		this.additionalAllowedFunctions = additionalAllowedFunctions;
	}

	public static Neo4jSort of(Sort sort, String... allowedFunctions) {
		if (sort instanceof Neo4jSort neo4jSort) {
			return new Neo4jSort(neo4jSort.unwrap(), Set.of(allowedFunctions));
		}
		return new Neo4jSort(sort, Set.of(allowedFunctions));
	}

	public Set<String> getAllowedFunctions() {
		var allowedFunctions = new HashSet<>(this.additionalAllowedFunctions);
		allowedFunctions.addAll(DEFAULT_ALLOWED_FUNCTIONS);
		return allowedFunctions;
	}

	Sort unwrap() {
		return this.wrapped;
	}

	@Override
	public Set<Order> toSet() {
		return this.wrapped.toSet();
	}

	@Override
	public List<Order> toList() {
		return this.wrapped.toList();
	}

	@Override
	public Stream<Order> stream() {
		return this.wrapped.stream();
	}

	@Override
	public <R> Streamable<R> map(Function<? super Order, ? extends R> mapper) {
		return this.wrapped.map(mapper);
	}

	@Override
	public Stream<Order> get() {
		return this.wrapped.get();
	}

	@Override
	public <R> Streamable<R> flatMap(Function<? super Order, ? extends Stream<? extends R>> mapper) {
		return this.wrapped.flatMap(mapper);
	}

	@Override
	public Streamable<Order> filter(Predicate<? super Order> predicate) {
		return this.wrapped.filter(predicate);
	}

	@Override
	public Streamable<Order> and(Streamable<? extends Order> streamable) {
		return this.wrapped.and(streamable);
	}

	@Override
	public Streamable<Order> and(Supplier<? extends Stream<? extends Order>> stream) {
		return this.wrapped.and(stream);
	}

	@Override
	public Streamable<Order> and(Order... others) {
		return this.wrapped.and(others);
	}

	@Override
	public Streamable<Order> and(Iterable<? extends Order> iterable) {
		return this.wrapped.and(iterable);
	}

}
