/*
 * Copyright 2011-2020 the original author or authors.
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

import java.util.EmptyStackException;

import org.neo4j.ogm.metadata.MetaData;
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

	protected final GraphQueryMethod queryMethod;
	protected final MetaData metaData;
	protected final Session session;

	protected AbstractGraphRepositoryQuery(GraphQueryMethod queryMethod, MetaData metaData, Session session) {

		this.queryMethod = queryMethod;
		this.metaData = metaData;
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
		return queryMethod;
	}

	protected GraphQueryExecution getExecution(GraphParameterAccessor accessor) {

		if (queryMethod.isStreamQuery()) {
			return new GraphQueryExecution.CollectionExecution(session, accessor);
		}
		if (isCountQuery()) {
			return new GraphQueryExecution.CountByExecution(session);
		}
		if (isDeleteQuery()) {
			return new GraphQueryExecution.DeleteByExecution(session, queryMethod);
		}
		if (isExistsQuery()) {
			return new GraphQueryExecution.ExistsByExecution(session);
		}
		if (returnsOgmSpecificType()) {
			return new GraphQueryExecution.QueryResultExecution(session, accessor);
		}
		if (queryMethod.isCollectionQuery()) {
			return new GraphQueryExecution.CollectionExecution(session, accessor);
		}
		if (queryMethod.isPageQuery()) {
			return new GraphQueryExecution.PagedExecution(session, accessor);
		}
		if (queryMethod.isSliceQuery()) {
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
		Class returnType = queryMethod.getMethod().getReturnType();
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
