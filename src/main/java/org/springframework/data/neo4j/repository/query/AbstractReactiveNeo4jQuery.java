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

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.PropertyFilterSupport;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.DtoInstantiatingConverter;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.Nullable;
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

		Assert.notNull(neo4jOperations, "The Neo4j operations are required.");
		this.neo4jOperations = neo4jOperations;
		this.factory = factory;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	@Override
	public final Object execute(Object[] parameters) {

		Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor((Neo4jQueryMethod.Neo4jParameters) this.queryMethod.getParameters(), parameters);
		ResultProcessor resultProcessor = queryMethod.getResultProcessor().withDynamicProjection(parameterAccessor);

		ReturnedType returnedType = resultProcessor.getReturnedType();
		PreparedQuery<?> preparedQuery = prepareQuery(returnedType.getReturnedType(),
				PropertyFilterSupport.getInputProperties(resultProcessor, factory, mappingContext), parameterAccessor,
				null, getMappingFunction(resultProcessor));

		Object rawResult = new Neo4jQueryExecution.ReactiveQueryExecution(neo4jOperations).execute(preparedQuery,
				queryMethod.isCollectionLikeQuery());

		Converter<Object, Object> preparingConverter = OptionalUnwrappingConverter.INSTANCE;
		if (returnedType.isProjecting()) {
			DtoInstantiatingConverter converter = new DtoInstantiatingConverter(returnedType.getReturnedType(), mappingContext);

			// Neo4jQuerySupport ensure we will get an EntityInstanceWithSource in the projecting case
			preparingConverter = source -> converter.convert(
					(EntityInstanceWithSource) OptionalUnwrappingConverter.INSTANCE.convert(source));
		}

		return resultProcessor.processResult(rawResult, preparingConverter);
	}

	protected abstract <T extends Object> PreparedQuery<T> prepareQuery(Class<T> returnedType,
				Map<PropertyPath, Boolean> includedProperties, Neo4jParameterAccessor parameterAccessor,
				@Nullable Neo4jQueryType queryType, @Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction);
}
