/*
 * Copyright 2011-2023 the original author or authors.
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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Window;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.neo4j.core.TemplateSupport;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Some conversions used by both reactive and imperative Neo4j queries. While we try to separate reactive and imperative
 * flows, it is cumbersome to repeat those conversions all over the place.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
abstract class Neo4jQuerySupport {

	protected static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();

	protected final Neo4jMappingContext mappingContext;
	protected final Neo4jQueryMethod queryMethod;
	/**
	 * The query type.
	 */
	protected final Neo4jQueryType queryType;
	private static final Set<Class<?>> VALID_RETURN_TYPES_FOR_DELETE = Collections.unmodifiableSet(new HashSet<>(
			Arrays.asList(Long.class, long.class, Void.class, void.class)));

	static final LogAccessor REPOSITORY_QUERY_LOG = new LogAccessor(LogFactory.getLog(Neo4jQuerySupport.class));

	/**
	 * Centralizes inquiry of the domain type to use the result processor of the query method as the point of truth.
	 * While this could be exposed on the query method itself, we would risk working with another type if at some point
	 * we osk the result processor only.
	 *
	 * @param queryMethod The query method whose domain type is requested
	 * @return The domain type of the given query method.
	 */
	static Class<?> getDomainType(QueryMethod queryMethod) {
		return queryMethod.getResultProcessor().getReturnedType().getDomainType();
	}

	Neo4jQuerySupport(Neo4jMappingContext mappingContext, Neo4jQueryMethod queryMethod, Neo4jQueryType queryType) {

		Assert.notNull(mappingContext, "The mapping context is required");
		Assert.notNull(queryMethod, "Query method must not be null");
		Assert.notNull(queryType, "Query type must not be null");
		Assert.isTrue(queryType != Neo4jQueryType.DELETE || hasValidReturnTypeForDelete(queryMethod),
				"A derived delete query can only return the number of deleted nodes as a long or void"
		);

		this.mappingContext = mappingContext;
		this.queryMethod = queryMethod;
		this.queryType = queryType;
	}

	protected final Supplier<BiFunction<TypeSystem, MapAccessor, ?>> getMappingFunction(final ResultProcessor resultProcessor) {

		return () -> {
			final ReturnedType returnedTypeMetadata = resultProcessor.getReturnedType();
			final Class<?> returnedType = returnedTypeMetadata.getReturnedType();
			final Class<?> domainType = returnedTypeMetadata.getDomainType();

			final BiFunction<TypeSystem, MapAccessor, ?> mappingFunction;

			if (mappingContext.getConversionService().isSimpleType(returnedType)) {
				// Clients automatically selects a single value mapping function.
				// It will throw an error if the query contains more than one column.
				mappingFunction = null;
			} else if (returnedTypeMetadata.isProjecting()) {
				mappingFunction = EntityInstanceWithSource.decorateMappingFunction(
						this.mappingContext.getRequiredMappingFunctionFor(domainType));
			} else {
				mappingFunction = this.mappingContext.getRequiredMappingFunctionFor(domainType);
			}
			return mappingFunction;
		};
	}

	private static boolean hasValidReturnTypeForDelete(Neo4jQueryMethod queryMethod) {
		return VALID_RETURN_TYPES_FOR_DELETE.contains(queryMethod.getResultProcessor().getReturnedType().getReturnedType());
	}

	static void logParameterIfNull(String name, Object value) {

		if (value != null || !REPOSITORY_QUERY_LOG.isDebugEnabled()) {
			return;
		}

		Supplier<CharSequence> messageSupplier = () -> {
			String pointer = name == null || name.trim().isEmpty() ? "An unknown parameter" : "$" + name;
			return String.format("%s points to a literal `null` value during a comparison. " +
							"The comparisons will always resolve to false and probably lead to an empty result.",
					pointer);
		};
		REPOSITORY_QUERY_LOG.debug(messageSupplier);
	}

	/**
	 * Converts parameter as needed by the query generated, which is not covered by standard conversion services.
	 *
	 * @param parameter The parameter to fit into the generated query.
	 * @return A parameter that fits the placeholders of a generated query
	 */
	final Object convertParameter(Object parameter) {
		return this.convertParameter(parameter, null);
	}

	/**
	 * Converts parameter as needed by the query generated, which is not covered by standard conversion services.
	 *
	 * @param parameter The parameter to fit into the generated query.
	 * @param conversionOverride Passed to the entity converter if present.
	 * @return A parameter that fits the placeholders of a generated query
	 */
	final Object convertParameter(Object parameter, @Nullable Neo4jPersistentPropertyConverter<?> conversionOverride) {

		if (parameter == null) {
			return Values.NULL;
		} else if (parameter instanceof Range v) {
			return convertRange(v);
		} else if (parameter instanceof Distance v) {
			return calculateDistanceInMeter(v);
		} else if (parameter instanceof Circle v) {
			return convertCircle(v);
		} else if (parameter instanceof Instant v) {
			return v.atOffset(ZoneOffset.UTC);
		} else if (parameter instanceof Box v) {
			return convertBox(v);
		} else if (parameter instanceof BoundingBox v) {
			return convertBoundingBox(v);
		}

		if (parameter instanceof Collection<?> col) {
			Class<?> type = TemplateSupport.findCommonElementType(col);
			if (type != null && mappingContext.hasPersistentEntityFor(type)) {

				EntityWriter<Object, Map<String, Object>> objectMapEntityWriter = Neo4jNestedMapEntityWriter
						.forContext(mappingContext);

				return col.stream().map(v -> {
					Map<String, Object> result = new HashMap<>();
					objectMapEntityWriter.write(v, result);
					return result;
				}).collect(Collectors.toList());
			}
		}

		if (mappingContext.hasPersistentEntityFor(parameter.getClass())) {

			Map<String, Object> result = new HashMap<>();
			Neo4jNestedMapEntityWriter.forContext(mappingContext).write(parameter, result);
			return result;
		}

		return mappingContext.getConversionService().writeValue(parameter,
				TypeInformation.of(parameter.getClass()), conversionOverride);
	}

	static class QueryContext {

		final String repositoryMethodName;

		final String template;

		final Map<String, Object> boundParameters;

		String query;

		private boolean hasLiteralReplacementForSort = false;

		QueryContext(String repositoryMethodName, String template, Map<String, Object> boundParameters) {
			this.repositoryMethodName = repositoryMethodName;
			this.template = template;
			this.query = this.template;
			this.boundParameters = boundParameters;
		}
	}

	void replaceLiteralsIn(QueryContext queryContext) {

		String cypherQuery = queryContext.template;
		Iterator<Map.Entry<String, Object>> iterator = queryContext.boundParameters.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Object> entry = iterator.next();
			Object value = entry.getValue();
			if (!(value instanceof Neo4jSpelSupport.LiteralReplacement)) {
				continue;
			}
			iterator.remove();

			String key = entry.getKey();
			cypherQuery = cypherQuery.replace("$" + key, ((Neo4jSpelSupport.LiteralReplacement) value).getValue());
			queryContext.hasLiteralReplacementForSort =
					queryContext.hasLiteralReplacementForSort ||
					((Neo4jSpelSupport.LiteralReplacement) value).getTarget() == Neo4jSpelSupport.LiteralReplacement.Target.SORT;
		}
		queryContext.query = cypherQuery;
	}

	void logWarningsIfNecessary(QueryContext queryContext, Neo4jParameterAccessor parameterAccessor) {

		// Log warning if necessary
		if (!(queryContext.hasLiteralReplacementForSort || parameterAccessor.getSort().isUnsorted())) {

			Neo4jQuerySupport.REPOSITORY_QUERY_LOG.warn(() ->
					String.format(
							"You passed a sorted request to the custom query for '%s'. SDN won't apply any sort information from that object to the query. "
							+ "Please specify the order in the query itself and use an unsorted request or use the SpEL extension `:#{orderBy(#sort)}`.",
							queryContext.repositoryMethodName));

			String fragment = CypherGenerator.INSTANCE.createOrderByFragment(parameterAccessor.getSort());
			if (fragment != null) {
				Neo4jQuerySupport.REPOSITORY_QUERY_LOG.warn(() ->
						String.format(
								"One possible order clause matching your page request would be the following fragment:%n%s",
								fragment));
			}
		}
	}

	final Window<?> createWindow(ResultProcessor resultProcessor, boolean incrementLimit, Neo4jParameterAccessor parameterAccessor, List<?> rawResult, QueryFragmentsAndParameters orderBy) {

		var domainType = resultProcessor.getReturnedType().getDomainType();
		var neo4jPersistentEntity = mappingContext.getPersistentEntity(domainType);
		var limit = orderBy.getQueryFragments().getLimit().intValue() - (incrementLimit ? 1 : 0);
		var conversionService = mappingContext.getConversionService();
		var scrollPosition = parameterAccessor.getScrollPosition();

		var scrollDirection = scrollPosition instanceof KeysetScrollPosition keysetScrollPosition ? keysetScrollPosition.getDirection() : KeysetScrollPosition.Direction.Forward;
		if (scrollDirection == KeysetScrollPosition.Direction.Backward) {
			Collections.reverse(rawResult);
		}

		return Window.from(getSubList(rawResult, limit, scrollDirection), v -> {
			if (scrollPosition instanceof OffsetScrollPosition offsetScrollPosition) {
				return OffsetScrollPosition.of(offsetScrollPosition.getOffset() + v + limit);
			} else  {
				var accessor = neo4jPersistentEntity.getPropertyAccessor(rawResult.get(v));
				var keys = new LinkedHashMap<String, Object>();
				orderBy.getSort().forEach(o -> {
					// Storing the graph property name here
					var persistentProperty = neo4jPersistentEntity.getRequiredPersistentProperty(o.getProperty());
					keys.put(persistentProperty.getPropertyName(), conversionService.convert(accessor.getProperty(persistentProperty), Value.class));
				});
				keys.put(Constants.NAME_OF_ADDITIONAL_SORT, conversionService.convert(accessor.getProperty(neo4jPersistentEntity.getRequiredIdProperty()), Value.class));
				return KeysetScrollPosition.of(keys);
			}
		}, hasMoreElements(rawResult, limit));
	}

	private static boolean hasMoreElements(List<?> result, int limit) {
		return !result.isEmpty() && result.size() > limit;
	}

	private static <T> List<T> getSubList(List<T> result, int limit, KeysetScrollPosition.Direction scrollDirection) {

		if (limit > 0 && result.size() > limit) {
			return scrollDirection == KeysetScrollPosition.Direction.Forward ? result.subList(0, limit) : result.subList(1, limit + 1);
		}

		return result;
	}

	private Map<String, Object> convertRange(Range<?> range) {
		Map<String, Object> map = new HashMap<>();
		range.getLowerBound().getValue().map(this::convertParameter).ifPresent(v -> map.put("lb", v));
		range.getUpperBound().getValue().map(this::convertParameter).ifPresent(v -> map.put("ub", v));
		return map;
	}

	private Map<String, Object> convertCircle(Circle circle) {
		Map<String, Object> map = new HashMap<>();
		map.put("x", convertParameter(circle.getCenter().getX()));
		map.put("y", convertParameter(circle.getCenter().getY()));
		map.put("radius", convertParameter(calculateDistanceInMeter(circle.getRadius())));
		return map;
	}

	private Map<String, Object> convertBox(Box box) {

		BoundingBox boundingBox = BoundingBox.of(box);
		return convertBoundingBox(boundingBox);
	}

	private Map<String, Object> convertBoundingBox(BoundingBox boundingBox) {

		Map<String, Object> map = new HashMap<>();

		map.put("llx", convertParameter(boundingBox.getLowerLeft().getX()));
		map.put("lly", convertParameter(boundingBox.getLowerLeft().getY()));
		map.put("urx", convertParameter(boundingBox.getUpperRight().getX()));
		map.put("ury", convertParameter(boundingBox.getUpperRight().getY()));

		return map;
	}

	private static double calculateDistanceInMeter(Distance distance) {

		if (distance.getMetric() == Metrics.KILOMETERS) {
			double kilometersDivisor = 0.001d;
			return distance.getValue() / kilometersDivisor;

		} else if (distance.getMetric() == Metrics.MILES) {
			double milesDivisor = 0.00062137d;
			return distance.getValue() / milesDivisor;

		} else {
			return distance.getValue();
		}
	}
}
