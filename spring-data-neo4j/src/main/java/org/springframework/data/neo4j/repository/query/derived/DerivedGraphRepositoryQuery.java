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

package org.springframework.data.neo4j.repository.query.derived;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.repository.query.AbstractGraphRepositoryQuery;
import org.springframework.data.neo4j.repository.query.GraphParameterAccessor;
import org.springframework.data.neo4j.repository.query.GraphParametersParameterAccessor;
import org.springframework.data.neo4j.repository.query.GraphQueryMethod;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Specialisation of {@link RepositoryQuery} that handles mapping of derived finders.
 *
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Vince Bickers
 * @author Nicolas Mervaillie
 * @author Mark Paluch
 */
public class DerivedGraphRepositoryQuery extends AbstractGraphRepositoryQuery {

	private static final Logger LOG = LoggerFactory.getLogger(DerivedGraphRepositoryQuery.class);
	private final DerivedQueryDefinition queryDefinition;
	private final GraphQueryMethod graphQueryMethod;
	private final PartTree tree;

	public DerivedGraphRepositoryQuery(GraphQueryMethod graphQueryMethod, Session session) {
		super(graphQueryMethod, session);
		this.graphQueryMethod = graphQueryMethod;
		Class<?> domainType = graphQueryMethod.getEntityInformation().getJavaType();
		this.tree = new PartTree(graphQueryMethod.getName(), domainType);
		this.queryDefinition = new DerivedQueryCreator(tree, domainType).createQuery();
	}

	@Override
	protected Object doExecute(Query params, Object[] parameters) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query for method {}", graphQueryMethod.getName());
		}

		GraphParameterAccessor accessor = new GraphParametersParameterAccessor(graphQueryMethod, parameters);
		Class<?> returnType = graphQueryMethod.getMethod().getReturnType();

		if (returnType.equals(Void.class)) {
			throw new RuntimeException("Derived Queries must have a return type");
		}

		ResultProcessor processor = graphQueryMethod.getResultProcessor().withDynamicProjection(accessor);
		Object results = getExecution(accessor).execute(params, processor.getReturnedType().getDomainType());

		return processor.processResult(results);
	}

	@Override
	protected Query getQuery(Object[] parameters) {
		return new Query(resolveParams(parameters));
	}

	@Override
	protected boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	protected boolean isExistsQuery() {
		return tree.isExistsProjection();
	}

	@Override
	protected boolean isDeleteQuery() {
		return tree.isDelete();
	}

	/**
	 * Sets values from parameters supplied by the finder on {@link org.neo4j.ogm.cypher.Filter} built by the
	 * {@link GraphQueryMethod}
	 *
	 * @param parameters parameter values supplied by the finder method
	 * @return List of Parameter with values set
	 */
	private Filters resolveParams(Object[] parameters) {
		Map<Integer, Object> params = new HashMap<>();

		for (int i = 0; i < parameters.length; i++) {
			if (graphQueryMethod.getQueryDepthParamIndex() == null
					|| (graphQueryMethod.getQueryDepthParamIndex() != null && graphQueryMethod.getQueryDepthParamIndex() != i)) {
				params.put(i, parameters[i]);
			}
		}

		return new Filters(queryDefinition.getFilters(params));
	}
}
