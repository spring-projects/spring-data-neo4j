/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import java.util.EmptyStackException;

import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Base class for @link {@link RepositoryQuery}s.
 *
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
public abstract class AbstractGraphRepositoryQuery implements RepositoryQuery {

	private final GraphQueryMethod method;
	private final Session session;

	protected AbstractGraphRepositoryQuery(GraphQueryMethod method, Session session) {

		this.method = method;
		this.session = session;
	}

	protected abstract Query getQuery(Object[] parameters);

	@Override
	public Object execute(Object[] parameters) {

		Query query;
		try {
			query = getQuery(parameters);
		} catch (EmptyStackException e) {
			throw new IllegalArgumentException("Not enough arguments for query " + getQueryMethod().getName());
		}

		return doExecute(query, parameters);
	}

	protected abstract Object doExecute(Query params, Object[] parameters);

	@Override
	public GraphQueryMethod getQueryMethod() {
		return method;
	}

	protected GraphQueryExecution getExecution(GraphParameterAccessor accessor) {

		if (method.isStreamQuery()) {
			return new GraphQueryExecution.CollectionExecution(session, accessor);
		}
		if (isCountQuery()) {
			return new GraphQueryExecution.CountByExecution(session, accessor);
		}
		if (isDeleteQuery()) {
			return new GraphQueryExecution.DeleteByExecution(session, method, accessor);
		}
		if (returnsOgmSpecificType()) {
			return new GraphQueryExecution.QueryResultExecution(session, accessor);
		}
		if (method.isCollectionQuery()) {
			return new GraphQueryExecution.CollectionExecution(session, accessor);
		}
		if (method.isPageQuery()) {
			return new GraphQueryExecution.PagedExecution(session, accessor);
		}
		if (method.isSliceQuery()) {
			return new GraphQueryExecution.SlicedExecution(session, accessor);
		}
		return new GraphQueryExecution.SingleEntityExecution(session, accessor);
	}

	/**
	 * Does the query returns an OGM specific object type that should get a special processing ?
	 *
	 * @return true if that's the case
	 */
	private boolean returnsOgmSpecificType() {
		Class returnType = method.getMethod().getReturnType();
		return QueryStatistics.class.isAssignableFrom(returnType) || Result.class.isAssignableFrom(returnType);
	}

	/**
	 * Returns whether the query should get a count projection applied.
	 *
	 * @return
	 */
	protected abstract boolean isCountQuery();

	/**
	 * Returns whether the query should get an exists projection applied.
	 *
	 * @return
	 */
	protected abstract boolean isExistsQuery();

	/**
	 * Return weather the query should delete matching documents.
	 *
	 * @return
	 */
	protected abstract boolean isDeleteQuery();
}
