/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.PropertyFilterSupport;
import org.springframework.data.neo4j.core.mapping.DtoInstantiatingConverter;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
			Neo4jQueryMethod queryMethod,
			Neo4jQueryType queryType, ProjectionFactory factory) {

		super(mappingContext, queryMethod, queryType);
		this.factory = factory;

		Assert.notNull(neo4jOperations, "The Neo4j operations are required.");
		this.neo4jOperations = neo4jOperations;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	@Override
	public final Object execute(Object[] parameters) {

		boolean incrementLimit = queryMethod.isSliceQuery() && !queryMethod.getQueryAnnotation().map(q -> q.countQuery()).filter(StringUtils::hasText).isPresent();
		Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
				(Neo4jQueryMethod.Neo4jParameters) this.queryMethod.getParameters(),
				parameters);

		ResultProcessor resultProcessor = queryMethod.getResultProcessor().withDynamicProjection(parameterAccessor);
		ReturnedType returnedType = resultProcessor.getReturnedType();
		PreparedQuery<?> preparedQuery = prepareQuery(returnedType.getReturnedType(),
				PropertyFilterSupport.getInputProperties(resultProcessor, factory, mappingContext), parameterAccessor,
				null, getMappingFunction(resultProcessor), incrementLimit ? l -> l + 1 : UnaryOperator.identity());

		Object rawResult = new Neo4jQueryExecution.DefaultQueryExecution(neo4jOperations).execute(preparedQuery,
				queryMethod.isCollectionLikeQuery() || queryMethod.isPageQuery() || queryMethod.isSliceQuery());

		Converter<Object, Object> preparingConverter = OptionalUnwrappingConverter.INSTANCE;
		if (returnedType.isProjecting()) {
			DtoInstantiatingConverter converter = new DtoInstantiatingConverter(returnedType.getReturnedType(), mappingContext);

			// Neo4jQuerySupport ensure we will get an EntityInstanceWithSource in the projecting case
			preparingConverter = source -> converter.convert(
					(EntityInstanceWithSource) OptionalUnwrappingConverter.INSTANCE.convert(source));
		}

		if (queryMethod.isPageQuery()) {
			rawResult = createPage(parameterAccessor, (List<?>) rawResult);
		} else if (queryMethod.isSliceQuery()) {
			rawResult = createSlice(incrementLimit, parameterAccessor, (List<?>) rawResult);
		}
		return resultProcessor.processResult(rawResult, preparingConverter);
	}

	private Page<?> createPage(Neo4jParameterAccessor parameterAccessor, List<?> rawResult) {

		LongSupplier totalSupplier = () -> {

			Supplier<PreparedQuery<Long>> defaultCountQuery = () -> prepareQuery(Long.class,
					Collections.emptyMap(), parameterAccessor, Neo4jQueryType.COUNT, null, UnaryOperator.identity());
			PreparedQuery<Long> countQuery = getCountQuery(parameterAccessor).orElseGet(defaultCountQuery);

			return neo4jOperations.toExecutableQuery(countQuery).getRequiredSingleResult();
		};
		return PageableExecutionUtils.getPage(rawResult, parameterAccessor.getPageable(), totalSupplier);
	}

	private Slice<?> createSlice(boolean incrementLimit, Neo4jParameterAccessor parameterAccessor, List<?> rawResult) {

		Pageable pageable = parameterAccessor.getPageable();

		if (incrementLimit) {
			 return new SliceImpl<>(
					rawResult.subList(0, Math.min(rawResult.size(), pageable.getPageSize())),
					PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
					rawResult.size() > pageable.getPageSize()
			);
		} else {
			PreparedQuery<Long> countQuery = getCountQuery(parameterAccessor)
					.orElseGet(() -> prepareQuery(Long.class, Collections.emptyMap(), parameterAccessor,
							Neo4jQueryType.COUNT, null, UnaryOperator.identity()));
			long total = neo4jOperations.toExecutableQuery(countQuery).getRequiredSingleResult();
			return new SliceImpl<>(
					rawResult,
					pageable,
					pageable.getOffset() + pageable.getPageSize() < total
			);
		}
	}

	protected abstract <T extends Object> PreparedQuery<T> prepareQuery(Class<T> returnedType,
			Map<PropertyPath, Boolean> includedProperties, Neo4jParameterAccessor parameterAccessor,
			@Nullable Neo4jQueryType queryType,
			@Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction,
			@Nullable UnaryOperator<Integer> limitModifier);

	protected Optional<PreparedQuery<Long>> getCountQuery(Neo4jParameterAccessor parameterAccessor) {
		return Optional.empty();
	}
}
