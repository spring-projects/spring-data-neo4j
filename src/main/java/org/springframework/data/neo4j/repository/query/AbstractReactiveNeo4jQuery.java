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
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import reactor.core.publisher.Flux;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.PropertyFilterSupport;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.DtoInstantiatingConverter;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryQuery} implementations for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
abstract class AbstractReactiveNeo4jQuery extends Neo4jQuerySupport implements RepositoryQuery {

	protected final ReactiveNeo4jOperations neo4jOperations;

	private ProjectionFactory factory;

	AbstractReactiveNeo4jQuery(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			Neo4jQueryMethod queryMethod, Neo4jQueryType queryType, ProjectionFactory factory) {

		super(mappingContext, queryMethod, queryType);

		Assert.notNull(neo4jOperations, "The Neo4j operations are required");
		this.neo4jOperations = neo4jOperations;
		this.factory = factory;
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

		if (Flux.class.isAssignableFrom(returnType)) {
			TypeInformation<?> from = TypeInformation.fromReturnTypeOf(repositoryMethod);
			TypeInformation<?> componentType = from.getComponentType();
			return componentType != null && GeoResult.class.equals(componentType.getType());
		}

		return false;
	}

	@Override
	@Nullable public final Object execute(Object[] parameters) {

		boolean incrementLimit = this.queryMethod.incrementLimit();
		boolean geoNearQuery = isGeoNearQuery();
		Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
				(Neo4jQueryMethod.Neo4jParameters) this.queryMethod.getParameters(), parameters);
		ResultProcessor resultProcessor = this.queryMethod.getResultProcessor()
			.withDynamicProjection(parameterAccessor);

		ReturnedType returnedType = resultProcessor.getReturnedType();
		PreparedQuery<?> preparedQuery = prepareQuery(returnedType.getReturnedType(),
				PropertyFilterSupport.getInputProperties(resultProcessor, this.factory, this.mappingContext),
				parameterAccessor, null, getMappingFunction(resultProcessor, geoNearQuery),
				incrementLimit ? l -> l + 1 : UnaryOperator.identity());

		Object rawResult = new Neo4jQueryExecution.ReactiveQueryExecution(this.neo4jOperations).execute(preparedQuery,
				this.queryMethod.asCollectionQuery());

		Converter<Object, Object> preparingConverter = OptionalUnwrappingConverter.INSTANCE;
		if (returnedType.isProjecting()) {
			DtoInstantiatingConverter converter = new DtoInstantiatingConverter(returnedType.getReturnedType(),
					this.mappingContext);

			// Neo4jQuerySupport ensure we will get an EntityInstanceWithSource in the
			// projecting case
			preparingConverter = source -> {
				var intermediate = (EntityInstanceWithSource) OptionalUnwrappingConverter.INSTANCE.convert(source);
				return (intermediate != null) ? converter.convert(intermediate) : null;
			};
		}

		if (this.queryMethod.isScrollQuery()) {
			rawResult = ((Flux<?>) rawResult).collectList()
				.map(rawResultList -> createWindow(resultProcessor, incrementLimit, parameterAccessor, rawResultList,
						preparedQuery.getQueryFragmentsAndParameters()));
		}

		return resultProcessor.processResult(rawResult, preparingConverter);
	}

	protected abstract <T extends Object> PreparedQuery<T> prepareQuery(Class<T> returnedType,
			Collection<PropertyFilter.ProjectedPath> includedProperties, Neo4jParameterAccessor parameterAccessor,
			@Nullable Neo4jQueryType queryType, Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction,
			UnaryOperator<Integer> limitModifier);

}
