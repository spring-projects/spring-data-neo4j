/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.query.GraphQueryMethod;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Specialisation of {@link RepositoryQuery} that handles mapping of derived finders.
 *
 * @author Mark Angrish
 * @author Luanne Misquitta
 */
public class DerivedGraphRepositoryQuery implements RepositoryQuery {

	private DerivedQueryDefinition queryDefinition;

	private final GraphQueryMethod graphQueryMethod;

	protected final Session session;

	private final int DEFAULT_QUERY_DEPTH = 1;

	public DerivedGraphRepositoryQuery(GraphQueryMethod graphQueryMethod, Session session) {
		this.graphQueryMethod = graphQueryMethod;
		this.session = session;
		EntityMetadata<?> info = graphQueryMethod.getEntityInformation();
		PartTree tree = new PartTree(graphQueryMethod.getName(), info.getJavaType());
		this.queryDefinition = new DerivedQueryCreator(tree, info.getJavaType()).createQuery();
	}

	@Override
	public Object execute(Object[] parameters) {
		Class<?> returnType = graphQueryMethod.getMethod().getReturnType();
		Class<?> concreteType = graphQueryMethod.resolveConcreteReturnType();
		int queryDepth = DEFAULT_QUERY_DEPTH;

		if (graphQueryMethod.hasStaticDepth()) {
			queryDepth = graphQueryMethod.getQueryDepth();
		}
		else {
			if (graphQueryMethod.getQueryDepthParamIndex() != null) {
				queryDepth = (int) parameters[graphQueryMethod.getQueryDepthParamIndex()];
			}
		}

		Filters params = resolveParams(parameters);
		if (returnType.equals(Void.class)) {
			throw new RuntimeException("Derived Queries must have a return type");
		}

		if (Iterable.class.isAssignableFrom(returnType)) {
			return session.loadAll(concreteType, params, queryDepth);
		}

		Iterator<?> objectIterator = session.loadAll(returnType, params, queryDepth).iterator();
		if(objectIterator.hasNext()) {
			return objectIterator.next();
		}
		return null;
	}

	/**
	 * Sets values from  parameters supplied by the finder on {@link org.neo4j.ogm.cypher.Filter} built by the {@link GraphQueryMethod}
	 * @param parameters parameter values supplied by the finder method
	 * @return List of Parameter with values set
	 */
	private Filters resolveParams(Object[] parameters) {
		Map<Integer, Object> params = new HashMap<>();

		for (int i = 0; i < parameters.length; i++) {
			if (graphQueryMethod.getQueryDepthParamIndex() == null || (graphQueryMethod.getQueryDepthParamIndex() != null && graphQueryMethod.getQueryDepthParamIndex() != i)) {
				params.put(i, parameters[i]);
			}
		}
		List<CypherFilter> cypherFilters = queryDefinition.getCypherFilters();
		Filters queryParams = new Filters();
		for(CypherFilter cypherFilter : cypherFilters) {
			Filter filter = cypherFilter.toFilter();
			filter.setPropertyValue(params.get(cypherFilter.getPropertyPosition()));
			queryParams.add(filter);
		}
		return queryParams;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return graphQueryMethod;
	}
}
