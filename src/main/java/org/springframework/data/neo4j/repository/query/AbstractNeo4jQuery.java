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
package org.springframework.data.neo4j.repository.query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.PropertyFilterSupport;
import org.springframework.data.neo4j.core.mapping.DtoInstantiatingConverter;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryQuery} implementations for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
abstract class AbstractNeo4jQuery extends Neo4jQuerySupport implements RepositoryQuery {

	protected final Neo4jOperations neo4jOperations;

	private final ProjectionFactory factory;

	AbstractNeo4jQuery(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, Neo4jQueryType queryType, ProjectionFactory factory) {

		super(mappingContext, queryMethod, queryType);
		this.factory = factory;

		Assert.notNull(neo4jOperations, "The Neo4j operations are required");
		this.neo4jOperations = neo4jOperations;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	boolean isGeoNearQuery() {
		var repositoryMethod = this.queryMethod.getMethod();
		Class<?> returnType = repositoryMethod.getReturnType();

		for (Class<?> type : Neo4jQueryMethod.GEO_NEAR_RESULTS) {
			if (type.isAssignableFrom(returnType)) {
				return true;
			}
		}

		if (Iterable.class.isAssignableFrom(returnType)) {
			TypeInformation<?> from = TypeInformation.fromReturnTypeOf(repositoryMethod);
			return from.getComponentType() != null && GeoResult.class.equals(from.getComponentType().getType());
		}

		return GeoPage.class.isAssignableFrom(returnType);
	}

	boolean isVectorSearchQuery() {
		var repositoryMethod = this.queryMethod.getMethod();
		Class<?> returnType = repositoryMethod.getReturnType();

		for (Class<?> type : Neo4jQueryMethod.VECTOR_SEARCH_RESULTS) {
			if (type.isAssignableFrom(returnType)) {
				return true;
			}
		}

		if (Iterable.class.isAssignableFrom(returnType)) {
			TypeInformation<?> from = TypeInformation.fromReturnTypeOf(repositoryMethod);
			return from.getComponentType() != null && SearchResult.class.equals(from.getComponentType().getType());
		}

		return false;
	}

	@Override
	@Nullable public final Object execute(Object[] parameters) {

		boolean incrementLimit = this.queryMethod.incrementLimit();
		boolean geoNearQuery = isGeoNearQuery();
		boolean vectorSearchQuery = isVectorSearchQuery();
		Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
				(Neo4jQueryMethod.Neo4jParameters) this.queryMethod.getParameters(), parameters);

		ResultProcessor resultProcessor = this.queryMethod.getResultProcessor()
			.withDynamicProjection(parameterAccessor);
		ReturnedType returnedType = resultProcessor.getReturnedType();
		PreparedQuery<?> preparedQuery = prepareQuery(returnedType.getReturnedType(),
				PropertyFilterSupport.getInputProperties(resultProcessor, this.factory, this.mappingContext),
				parameterAccessor, null, getMappingFunction(resultProcessor, geoNearQuery, vectorSearchQuery),
				incrementLimit ? l -> l + 1 : UnaryOperator.identity());

		Object rawResult = new Neo4jQueryExecution.DefaultQueryExecution(this.neo4jOperations).execute(preparedQuery,
				this.queryMethod.asCollectionQuery());

		Converter<Object, Object> preparingConverter = OptionalUnwrappingConverter.INSTANCE;
		if (returnedType.isProjecting()) {
			DtoInstantiatingConverter converter = new DtoInstantiatingConverter(returnedType.getReturnedType(),
					this.mappingContext);

			// Neo4jQuerySupport ensure we will get an EntityInstanceWithSource in the
			// projecting case
			preparingConverter = source -> {
				var unwrapped = (EntityInstanceWithSource) OptionalUnwrappingConverter.INSTANCE.convert(source);
				return (unwrapped != null) ? converter.convert(unwrapped) : null;
			};
		}

		if (this.queryMethod.isPageQuery()) {
			rawResult = createPage(parameterAccessor, (List<?>) rawResult);
		}
		else if (this.queryMethod.isSliceQuery()) {
			rawResult = createSlice(incrementLimit, parameterAccessor, (List<?>) rawResult);
		}
		else if (this.queryMethod.isScrollQuery()) {
			rawResult = createWindow(resultProcessor, incrementLimit, parameterAccessor, (List<?>) rawResult,
					preparedQuery.getQueryFragmentsAndParameters());
		}
		else if (geoNearQuery) {
			rawResult = newGeoResults(rawResult);
		}
		else if (this.queryMethod.isSearchQuery()) {
			rawResult = createSearchResult((List<?>) rawResult);
		}

		return resultProcessor.processResult(rawResult, preparingConverter);
	}

	private Page<?> createPage(Neo4jParameterAccessor parameterAccessor, List<?> rawResult) {

		LongSupplier totalSupplier = () -> {

			Supplier<PreparedQuery<Long>> defaultCountQuery = () -> prepareQuery(Long.class, Collections.emptySet(),
					parameterAccessor, Neo4jQueryType.COUNT, null, UnaryOperator.identity());
			PreparedQuery<Long> countQuery = getCountQuery(parameterAccessor).orElseGet(defaultCountQuery);

			return this.neo4jOperations.toExecutableQuery(countQuery).getRequiredSingleResult();
		};

		if (isGeoNearQuery()) {
			return new GeoPage<>(newGeoResults(rawResult), parameterAccessor.getPageable(), totalSupplier.getAsLong());
		}

		return PageableExecutionUtils.getPage(rawResult, parameterAccessor.getPageable(), totalSupplier);
	}

	private Slice<?> createSlice(boolean incrementLimit, Neo4jParameterAccessor parameterAccessor, List<?> rawResult) {

		Pageable pageable = parameterAccessor.getPageable();

		if (incrementLimit) {
			return new SliceImpl<>(rawResult.subList(0, Math.min(rawResult.size(), pageable.getPageSize())),
					PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
					rawResult.size() > pageable.getPageSize());
		}
		else {
			PreparedQuery<Long> countQuery = getCountQuery(parameterAccessor).orElseGet(() -> prepareQuery(Long.class,
					Collections.emptySet(), parameterAccessor, Neo4jQueryType.COUNT, null, UnaryOperator.identity()));
			long total = this.neo4jOperations.toExecutableQuery(countQuery).getRequiredSingleResult();
			return new SliceImpl<>(rawResult, pageable, pageable.getOffset() + pageable.getPageSize() < total);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> SearchResults<?> createSearchResult(List<?> rawResult) {
		return new SearchResults<>(rawResult.stream().map(rawValue -> (SearchResult<T>) rawValue).toList());
	}

	protected abstract <T> PreparedQuery<T> prepareQuery(Class<T> returnedType,
			Collection<PropertyFilter.ProjectedPath> includedProperties, Neo4jParameterAccessor parameterAccessor,
			@Nullable Neo4jQueryType queryType,
			@Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction,
			UnaryOperator<Integer> limitModifier);

	protected Optional<PreparedQuery<Long>> getCountQuery(Neo4jParameterAccessor parameterAccessor) {
		return Optional.empty();
	}

}
