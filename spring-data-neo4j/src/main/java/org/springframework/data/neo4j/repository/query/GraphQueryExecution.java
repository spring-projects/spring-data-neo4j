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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import org.neo4j.ogm.model.Result;
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

	final class CountByExecution implements GraphQueryExecution {

		private final Session session;

		CountByExecution(Session session) {
			this.session = session;
		}

		@Override
		public Object execute(Query query, Class<?> type) {
			return session.count(type, query.getFilters());
		}
	}

	final class ExistsByExecution implements GraphQueryExecution {

		private final Session session;

		ExistsByExecution(Session session) {
			this.session = session;
		}

		@Override
		public Object execute(Query query, Class<?> type) {
			if (query.isFilterQuery()) {
				return session.count(type, query.getFilters()) > 0;
			}
			Result result = session.query(query.getCypherQuery(), query.getParameters());
			return result.iterator().hasNext();
		}
	}

	final class DeleteByExecution implements GraphQueryExecution {

		private final Session session;
		private final GraphQueryMethod graphQueryMethod;

		DeleteByExecution(Session session, GraphQueryMethod graphQueryMethod) {
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
