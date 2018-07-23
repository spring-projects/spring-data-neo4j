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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import org.neo4j.ogm.session.Session;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.Assert;

/**
 * Classes intended to pilot query execution according to the type of the query. The type of the query is determined by
 * looking at the result class of the method.
 *
 * @see AbstractGraphRepositoryQuery#getExecution(org.springframework.data.neo4j.repository.query.GraphParameterAccessor)
 * @author Nicolas Mervaillie
 */
public interface GraphQueryExecution {

	Object execute(Query query, Class<?> type);

	final class SingleEntityExecution implements GraphQueryExecution {

		private final Session session;
		private final GraphParameterAccessor accessor;

		SingleEntityExecution(Session session, GraphParameterAccessor accessor) {
			this.session = session;
			this.accessor = accessor;
		}

		@Override
		public Object execute(Query query, Class<?> type) {
			Iterable<?> result;
			if (query.isFilterQuery()) {
				result = session.loadAll(type, query.getFilters(), accessor.getDepth());
			} else {
				// not using queryForObject here because it raises too generic RuntimeException
				// if more than one result found
				if (type.getAnnotation(QueryResult.class) != null) {
					result = session.query(query.getCypherQuery(), query.getParameters()).queryResults();
				} else {
					result = session.query(type, query.getCypherQuery(), query.getParameters());
				}
			}
			Iterator<?> iterator = result.iterator();
			if (!iterator.hasNext()) {
				return null;
			}
			Object ret = iterator.next();
			if (iterator.hasNext()) {
				throw new IncorrectResultSizeDataAccessException("Incorrect result size: expected at most 1", 1);
			}
			return ret;
		}
	}

	final class CollectionExecution implements GraphQueryExecution {

		private final Session session;
		private final GraphParameterAccessor accessor;

		CollectionExecution(Session session, GraphParameterAccessor accessor) {
			this.session = session;
			this.accessor = accessor;
		}

		@Override
		public Object execute(Query query, Class<?> type) {
			if (query.isFilterQuery()) {
				return session.loadAll(type, query.getFilters(), accessor.getOgmSort(), accessor.getDepth());
			} else {
				if (type.getAnnotation(QueryResult.class) != null || Map.class.isAssignableFrom(type)) {
					return session.query(query.getCypherQuery(accessor.getSort()), query.getParameters()).queryResults();
				} else {
					return session.query(type, query.getCypherQuery(accessor.getSort()), query.getParameters());
				}
			}
		}
	}

	final class QueryResultExecution implements GraphQueryExecution {

		private final Session session;
		private final GraphParameterAccessor accessor;

		QueryResultExecution(Session session, GraphParameterAccessor accessor) {
			this.session = session;
			this.accessor = accessor;
		}

		@Override
		public Object execute(Query query, Class<?> type) {
			return session.query(query.getCypherQuery(accessor.getSort()), query.getParameters());
		}
	}

	final class PagedExecution implements GraphQueryExecution {

		private final Session session;
		private final Pageable pageable;
		private final GraphParameterAccessor accessor;

		PagedExecution(Session session, GraphParameterAccessor accessor) {
			this.session = session;
			this.pageable = accessor.getPageable();
			this.accessor = accessor;
		}

		@Override
		public Object execute(Query query, Class<?> type) {

			List<?> result;
			long count;
			if (query.isFilterQuery()) {
				result = (List<?>) session.loadAll(type, query.getFilters(), accessor.getOgmSort(),
						query.getPagination(pageable, false), accessor.getDepth());
				count = session.count(type, query.getFilters());
			} else {
				if (type.getAnnotation(QueryResult.class) != null) {
					result = (List<?>) session.query(query.getCypherQuery(pageable, false), query.getParameters()).queryResults();
				} else {
					result = (List<?>) session.query(type, query.getCypherQuery(pageable, false), query.getParameters());
				}
				count = (result.size() > 0) ? countTotalNumberOfElements(query) : 0;
			}

			return PageableExecutionUtils.getPage(result, pageable, (LongSupplier) () -> count);
		}

		private Integer countTotalNumberOfElements(Query query) {
			Assert.hasText(query.getCountQuery(), "Must specify a count query to get pagination info.");
			return session.queryForObject(Integer.class, query.getCountQuery(), query.getParameters());
		}
	}

	final class SlicedExecution implements GraphQueryExecution {

		private final Session session;
		private final GraphParameterAccessor accessor;
		private final Pageable pageable;

		SlicedExecution(Session session, GraphParameterAccessor accessor) {
			this.session = session;
			this.accessor = accessor;
			this.pageable = accessor.getPageable();
		}

		@Override
		public Object execute(Query query, Class<?> type) {

			int pageSize = pageable.getPageSize();

			List<?> result;
			if (query.isFilterQuery()) {
				// For a slice, need one extra result to determine if there is a next page
				result = (List<?>) session.loadAll(type, query.getFilters(), accessor.getOgmSort(),
						query.getPagination(pageable, true), accessor.getDepth());
			} else {
				String cypherQuery = query.getCypherQuery(pageable, true);
				if (type.getAnnotation(QueryResult.class) != null) {
					result = (List<?>) session.query(cypherQuery, query.getParameters()).queryResults();
				} else {
					result = (List<?>) session.query(type, cypherQuery, query.getParameters());
				}
			}

			boolean hasNext = result.size() > pageSize;
			return new SliceImpl(hasNext ? result.subList(0, pageSize) : result, pageable, hasNext);
		}
	}

	final class StreamExecution implements GraphQueryExecution {

		private final Session session;
		private final GraphParameterAccessor accessor;

		public StreamExecution(org.neo4j.ogm.session.Session session, GraphParameterAccessor accessor) {
			this.session = session;
			this.accessor = accessor;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object execute(Query query, Class<?> type) {

			// not a real support for streaming. for that need that the stack all the way down
			// supports streaming
			List<?> result;
			if (query.isFilterQuery()) {
				result = (List<?>) session.loadAll(type, query.getFilters(), accessor.getOgmSort(), accessor.getDepth());
			} else {
				// TODO add support for QueryResults as above
				result = (List<?>) session.query(type, query.getCypherQuery(accessor.getSort()), query.getParameters());
			}
			return result;
		}
	}

	final class CountByExecution implements GraphQueryExecution {

		private final Session session;

		CountByExecution(Session session, GraphParameterAccessor accessor) {
			this.session = session;
		}

		@Override
		public Object execute(Query query, Class<?> type) {
			return session.count(type, query.getFilters());
		}
	}

	final class DeleteByExecution implements GraphQueryExecution {

		private final Session session;
		private final GraphQueryMethod graphQueryMethod;

		DeleteByExecution(Session session, GraphQueryMethod graphQueryMethod, GraphParameterAccessor accessor) {
			this.session = session;
			this.graphQueryMethod = graphQueryMethod;
		}

		@Override
		public Object execute(Query query, Class<?> type) {
			Class<?> returnType = graphQueryMethod.getReturnedObjectType();

			if (returnType.equals(Long.class)) {
				return session.delete(type, query.getFilters(), graphQueryMethod.isCollectionQuery());
			}
			throw new RuntimeException("Long or Iterable<Long> is required as the return type of a Delete query");
		}
	}
}
