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

import static org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension.*;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.repository.query.spel.ParameterizedQuery;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Specialisation of {@link RepositoryQuery} that handles mapping to object annotated with <code>&#064;Query</code>.
 *
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Mark Paluch
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
public class GraphRepositoryQuery extends AbstractGraphRepositoryQuery {

	private static final Logger LOG = LoggerFactory.getLogger(GraphRepositoryQuery.class);

	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private ParameterizedQuery parameterizedQuery;

	private final Optional<Constructor<?>> singleUseEntityMapperUsingInstantiator;
	private final Object entityInstantiator;

	GraphRepositoryQuery(GraphQueryMethod graphQueryMethod, MetaData metaData, Session session,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		super(graphQueryMethod, metaData, session);

		this.evaluationContextProvider = evaluationContextProvider;
		if (HAS_ENTITY_INSTANTIATOR_FEATURE) {
			this.entityInstantiator = getEntityInstantiator(metaData, queryMethod.getMappingContext());
			this.singleUseEntityMapperUsingInstantiator = ReflectionUtils.findConstructor(SingleUseEntityMapper.class,
					metaData, entityInstantiator);
		} else {
			this.entityInstantiator = null;
			this.singleUseEntityMapperUsingInstantiator = Optional.empty();
		}
	}

	protected Object doExecute(Query query, Object[] parameters) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query for method {}", queryMethod.getName());
		}

		GraphParameterAccessor accessor = new GraphParametersParameterAccessor(queryMethod, parameters);
		Class<?> returnType = queryMethod.getMethod().getReturnType();

		ResultProcessor processor = queryMethod.getResultProcessor().withDynamicProjection(accessor);

		Object result = getExecution(accessor).execute(query, processor.getReturnedType().getReturnedType());

		return Result.class.equals(returnType) ? result
				: processor.processResult(result, new CustomResultConverter(metaData,
						processor.getReturnedType().getReturnedType(), singleUseEntityMapperUsingInstantiator, entityInstantiator));
	}

	protected Query getQuery(Object[] parameters) {
		ParameterizedQuery parameterizedQuery = getParameterizedQuery();
		Map<String, Object> parametersFromQuery = parameterizedQuery.resolveParameter(parameters, this::resolveParams);
		return new Query(parameterizedQuery.getQueryString(), queryMethod.getCountQueryString(), parametersFromQuery);
	}

	private ParameterizedQuery getParameterizedQuery() {
		if (parameterizedQuery == null) {

			Parameters<?, ?> methodParameters = queryMethod.getParameters();
			parameterizedQuery = ParameterizedQuery.getParameterizedQuery(getAnnotationQueryString(), methodParameters,
					evaluationContextProvider);
		}
		return parameterizedQuery;
	}

	Map<String, Object> resolveParams(Parameters<?, ?> methodParameters, Object[] parameters) {

		Map<String, Object> resolvedParameters = new HashMap<>();

		for (Parameter parameter : methodParameters) {
			int parameterIndex = parameter.getIndex();
			Object parameterValue = getParameterValue(parameters[parameterIndex]);

			// We support using parameters based on their index and their name at the same time,
			// so parameters are always bound by index.
			resolvedParameters.put(Integer.toString(parameterIndex), parameterValue);

			// Make sure we don't add "special" parameters as named parameters
			if (parameter.isNamedParameter()) {
				// even though the above check ensures the presence usually, it's probably better to
				// treat #isNamedParameter as a blackbox and not just calling #get() on the optional.
				parameter.getName().ifPresent(parameterName -> resolvedParameters.put(parameterName, parameterValue));
			}
		}

		return resolvedParameters;
	}

	private String getAnnotationQueryString() {
		return getQueryMethod().getQuery();
	}

	private Object getParameterValue(Object parameter) {

		// The parameter might be an entity, try to resolve its id
		Object parameterValue = session.resolveGraphIdFor(parameter);
		if (parameterValue == null) { // Either not an entity or not persisted
			parameterValue = parameter;
		}
		return parameterValue;
	}

	@Override
	protected boolean isCountQuery() {
		return false;
	}

	@Override
	protected boolean isExistsQuery() {
		return false;
	}

	@Override
	protected boolean isDeleteQuery() {
		return false;
	}

	private static Object getEntityInstantiator(MetaData metaData,
			@Nullable MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {

		try {
			Class<?> queryResultInstantiatorClass = Class.forName(
					"org.springframework.data.neo4j.repository.query.QueryResultInstantiator", true,
					ClassUtils.getDefaultClassLoader());
			Optional<Constructor<?>> queryResultInstantiatorConstructor = ReflectionUtils
					.findConstructor(queryResultInstantiatorClass, metaData, mappingContext);
			return queryResultInstantiatorConstructor.map(ctor -> BeanUtils.instantiateClass(ctor, metaData, mappingContext))
					.get();
		} catch (ClassNotFoundException e) {
			// It is there, we just don't want to reference it directly.
			return null;
		}
	}
}
