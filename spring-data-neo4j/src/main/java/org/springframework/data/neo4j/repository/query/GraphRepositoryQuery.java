/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.query;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.repository.query.spel.ParameterizedQuery;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;

/**
 * Specialisation of {@link RepositoryQuery} that handles mapping to object annotated with <code>&#064;Query</code>.
 *
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Mark Paluch
 * @author Nicolas Mervaillie
 */
public class GraphRepositoryQuery extends AbstractGraphRepositoryQuery {

	private static final Logger LOG = LoggerFactory.getLogger(GraphRepositoryQuery.class);

	private final GraphQueryMethod graphQueryMethod;
	private final Session session;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private ParameterizedQuery parameterizedQuery;

	GraphRepositoryQuery(GraphQueryMethod graphQueryMethod, Session session,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		super(graphQueryMethod, session);
		this.graphQueryMethod = graphQueryMethod;
		this.session = session;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	protected Object doExecute(Query query, Object[] parameters) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query for method {}", graphQueryMethod.getName());
		}

		GraphParameterAccessor accessor = new GraphParametersParameterAccessor(graphQueryMethod, parameters);
		Class<?> returnType = graphQueryMethod.getMethod().getReturnType();

		ResultProcessor processor = graphQueryMethod.getResultProcessor().withDynamicProjection(accessor);

		Object result = getExecution(accessor).execute(query, processor.getReturnedType().getReturnedType());

		return Result.class.equals(returnType) ? result
				: processor.processResult(result,
						new CustomResultConverter(getMetaData(), processor.getReturnedType().getReturnedType()));
	}

	protected Query getQuery(Object[] parameters) {
		ParameterizedQuery parameterizedQuery = getParameterizedQuery();
		Map<String, Object> parametersFromQuery = parameterizedQuery.resolveParameter(parameters, this::resolveParams);
		return new Query(parameterizedQuery.getQueryString(), graphQueryMethod.getCountQueryString(), parametersFromQuery);
	}

	private ParameterizedQuery getParameterizedQuery() {
		if (parameterizedQuery == null) {

			Parameters<?, ?> methodParameters = graphQueryMethod.getParameters();
			parameterizedQuery = ParameterizedQuery.getParameterizedQuery(getAnnotationQueryString(), methodParameters,
					evaluationContextProvider);
		}
		return parameterizedQuery;
	}

	Map<String, Object> resolveParams(Parameters<?, ?> methodParameters, Object[] parameters) {
		Map<String, Object> params = new HashMap<>();

		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = methodParameters.getParameter(i);
			Object parameterValue = getParameterValue(parameters[i]);

			if (parameter.isExplicitlyNamed()) {
				parameter.getName().ifPresent(name -> params.put(name, parameterValue));
			}
			params.put("" + i, parameterValue);
		}

		return params;
	}

	// just an horrible trick to get the metadata from OGM
	private MetaData getMetaData() {
		return session.doInTransaction((requestHandler, transaction, metaData) -> metaData);
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

	GraphQueryMethod getGraphQueryMethod() {
		return graphQueryMethod;
	}

}
