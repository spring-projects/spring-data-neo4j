/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.repository.query.derived;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.cypher.Parameter;
import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.query.GraphQueryMethod;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 */
public class DerivedGraphRepositoryQuery implements RepositoryQuery {

	private DerivedQueryDefinition queryDefinition;

	private final GraphQueryMethod graphQueryMethod;

	protected final Session session;

	public DerivedGraphRepositoryQuery(GraphQueryMethod graphQueryMethod, RepositoryMetadata metadata, Session session) {
		this.graphQueryMethod = graphQueryMethod;
		this.session = session;
		EntityMetadata<?> info = graphQueryMethod.getEntityInformation();
		PartTree tree = new PartTree(graphQueryMethod.getName(), info.getJavaType());
		this.queryDefinition = new DerivedQueryCreator(tree, info.getJavaType()).createQuery();
	}

	protected Object execute(Class<?> returnType, Class<?> concreteType, String cypherQuery, Map<String, Object> queryParams) {
		if (returnType.equals(Void.class)) { //TODO this returns statistics now, what do we do with it?
			session.execute(cypherQuery, queryParams);
			return null;
		}

		if (Iterable.class.isAssignableFrom(returnType)) {
			// Special method to handle SDN Iterable<Map<String, Object>> behaviour.
			// TODO: Do we really want this method in an OGM? It's a little too low level and/or doesn't really fit.
			if (Map.class.isAssignableFrom(concreteType)) {
				return session.query(cypherQuery, queryParams);
			}
			return session.query(concreteType, cypherQuery, queryParams);
		}

		return session.queryForObject(returnType, cypherQuery, queryParams);
	}

	@Override
	public Object execute(Object[] parameters) {
		Class<?> returnType = graphQueryMethod.getMethod().getReturnType();
		Class<?> concreteType = graphQueryMethod.resolveConcreteReturnType();

		List<Parameter> params = resolveParams(parameters); //todo review this
		if (returnType.equals(Void.class)) {
			throw new RuntimeException("Derived Queries must have a return type");
		}

		if (Iterable.class.isAssignableFrom(returnType)) {
			/*// Special method to handle SDN Iterable<Map<String, Object>> behaviour.
			// TODO: Do we really want this method in an OGM? It's a little too low level and/or doesn't really fit.
			if (Map.class.isAssignableFrom(concreteType)) {
				return session.query(cypherQuery, queryParams);
			}*/
			return session.loadByProperties(concreteType, params);
		}

		Iterator objectIterator = session.loadByProperties(returnType, params).iterator(); //todo test for empty resultset
		if(objectIterator.hasNext()) {
			return objectIterator.next();
		}
		return null;
		//return execute(returnType, concreteType, getQueryString(), params);
	}

	private List<Parameter> resolveParams(Object[] parameters) {
		//Create a map of the parameter position -> parameter value
		Map<Integer, Object> params = new HashMap<>();
		Parameters<?, ?> methodParameters = graphQueryMethod.getParameters();

		for (int i = 0; i < parameters.length; i++) {
			org.springframework.data.repository.query.Parameter parameter = methodParameters.getParameter(i);
			params.put(i, parameters[i]);
		}

		List<Parameter> queryParams = queryDefinition.getQueryParameters();
		for(Parameter queryParam : queryParams) {
			queryParam.setPropertyValue(params.get(queryParam.getPropertyPosition()));
		}
		return queryParams;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return graphQueryMethod;
	}
}
