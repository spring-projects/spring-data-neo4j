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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

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
	private final EvaluationContextProvider evaluationContextProvider;

	public GraphRepositoryQuery(GraphQueryMethod graphQueryMethod, Session session,
			EvaluationContextProvider evaluationContextProvider) {
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
		ParameterizedQuery cypherQuery = getParameterizedQuery();
		Map<String, Object> cypherParameters = resolveParams(parameters, cypherQuery.placeholders);

		return new Query(cypherQuery.queryString, graphQueryMethod.getCountQueryString(), cypherParameters);
	}

	Map<String, Object> resolveParams(Object[] parameters, Map<String, String> placeholders) {
		System.out.println(Arrays.toString(parameters));
		System.out.println(placeholders);
		Map<String, Object> params = new HashMap<>();
		Parameters<?, ?> methodParameters = graphQueryMethod.getParameters();
		EvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(methodParameters, parameters);

		Set<Parameter> usedParameters = new HashSet<>();


		for (String placeholderKey : placeholders.keySet()) {
			AtomicBoolean found = new AtomicBoolean(false);
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = methodParameters.getParameter(i);
				Object parameterValue = getParameterValue(parameters[i]);

				if (parameter.isExplicitlyNamed()) {
					parameter.getName().ifPresent(name -> {
						String placeHolderString = placeholders.get(placeholderKey);
						if (placeholderKey.startsWith(name) && !placeholderKey.equals(name)) {
							Object value = new SpelExpressionParser()
									.parseExpression("#"+placeholderKey).getValue(evaluationContext, Object.class);
							params.put(placeHolderString, value);
						} else {
							params.put(placeHolderString, parameterValue);
						}
						found.set(true);
						usedParameters.add(parameter);

					});
				} else {
					Object value = new SpelExpressionParser()
							.parseExpression(placeholderKey.substring(placeholderKey.indexOf(".") + 1)).getValue(parameterValue);
					params.put("" + i, value);
					found.set(true);
					usedParameters.add(parameter);
				}
			}
			// unmapped placeholders in query string e.g. pure SpEl values like 5 + 5
			if (!found.get()) {
				try {
					Object value = new SpelExpressionParser().parseExpression(placeholderKey).getValue();
					String placeHolderString = placeholders.get(placeholderKey);
					params.put(placeHolderString, value);
				} catch (Exception e) {
					/// asdf
				}
			}

		}

		// unmapped parameters in method signature because no SpEl placeholder matches
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = methodParameters.getParameter(i);
//			if (usedParameters.contains(parameter)) {
//				continue;
//			}
			Object parameterValue = getParameterValue(parameters[i]);

//			if (parameter.isExplicitlyNamed()) {
				parameter.getName().ifPresent(name -> params.put(name, parameterValue));
//			} else {
				params.put("" + i, parameterValue);
//			}
		}

		return params;
	}

	// just an horrible trick to get the metadata from OGM
	private MetaData getMetaData() {
		return session.doInTransaction((requestHandler, transaction, metaData) -> metaData);
	}

	private ParameterizedQuery getParameterizedQuery() {
		String queryString = getQueryString();
		System.out.println(queryString);
		String spElIndexColon = ":#{[";
		String spElIndexQuestionMark = "?#{[";

		String spElObjectColon = ":#{#";
		String spElObjectQuestionMark = "?#{#";
		String spElValueColon = ":#{";
		String spElValueQuestionMark = "?#{";

		HashMap<String, String> placeholderMapping = new HashMap<>();

		int objectIndex;
		int placeholderIndex = Integer.MAX_VALUE;
		// index
		while ((objectIndex = queryString.indexOf(spElIndexColon)) > -1) {
			String placeHolderInQueryAnnotation = queryString.substring(objectIndex, queryString.indexOf("]}", objectIndex) + 2);
			String givenIndex = queryString.substring(objectIndex + 4, queryString.indexOf("]"));
			// just replace the SpEL index with native index placeholder
			queryString = queryString.replace(placeHolderInQueryAnnotation, "{" + givenIndex + "}");
		}
		while ((objectIndex = queryString.indexOf(spElIndexQuestionMark)) > -1) {
			String placeHolderInQueryAnnotation = queryString.substring(objectIndex, queryString.indexOf("]}", objectIndex) + 2);
			String givenIndex = queryString.substring(objectIndex + 4, queryString.indexOf("]"));
			// just replace the SpEL index with native index placeholder
			queryString = queryString.replace(placeHolderInQueryAnnotation, "{" + givenIndex + "}");
		}
		// object
		while ((objectIndex = queryString.indexOf(spElObjectColon)) > -1) {
			String placeHolderInQueryAnnotation = queryString.substring(objectIndex,
					queryString.indexOf("}", objectIndex) + 1);
			queryString = queryString.replace(placeHolderInQueryAnnotation, "{" + placeholderIndex + "}");
			placeholderMapping.put(placeHolderInQueryAnnotation.replace(spElObjectColon, "").replace("}", ""),
					"" + placeholderIndex--);

		}
		while ((objectIndex = queryString.indexOf(spElObjectQuestionMark)) > -1) {
			String placeHolderInQueryAnnotation = queryString.substring(objectIndex,
					queryString.indexOf("}", objectIndex) + 1);
			queryString = queryString.replace(placeHolderInQueryAnnotation, "{" + placeholderIndex + "}");
			placeholderMapping.put(placeHolderInQueryAnnotation.replace(spElObjectQuestionMark, "").replace("}", ""),
					"" + placeholderIndex--);
		}
		// value
		while ((objectIndex = queryString.indexOf(spElValueColon)) > -1) {
			String placeHolderInQueryAnnotation = queryString.substring(objectIndex,
					queryString.indexOf("}", objectIndex) + 1);
			queryString = queryString.replace(placeHolderInQueryAnnotation, "{" + placeholderIndex + "}");
			placeholderMapping.put(placeHolderInQueryAnnotation.replace(spElValueColon, "").replace("}", ""),
					"" + placeholderIndex--);

		}
		while ((objectIndex = queryString.indexOf(spElValueQuestionMark)) > -1) {
			String placeHolderInQueryAnnotation = queryString.substring(objectIndex,
					queryString.indexOf("}", objectIndex) + 1);
			queryString = queryString.replace(placeHolderInQueryAnnotation, "{" + placeholderIndex + "}");
			placeholderMapping.put(placeHolderInQueryAnnotation.replace(spElValueQuestionMark, "").replace("}", ""),
					"" + placeholderIndex--);
		}
		return new ParameterizedQuery(queryString, placeholderMapping);
	}

	private String getQueryString() {
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

	class ParameterizedQuery {
		final String queryString;
		final Map<String, String> placeholders;

		ParameterizedQuery(String queryString, Map<String, String> placeholders) {
			this.queryString = queryString;
			this.placeholders = placeholders;
			System.out.println(queryString);
		}
	}
}
