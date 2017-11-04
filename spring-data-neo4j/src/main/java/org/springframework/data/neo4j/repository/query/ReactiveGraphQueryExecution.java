package org.springframework.data.neo4j.repository.query;

import lombok.RequiredArgsConstructor;
import org.neo4j.ogm.session.Session;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.neo4j.annotation.QueryResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.Map;

/**
 * Set of classes to contain query execution strategies. Depending (mostly) on the return type of a
 * {@link org.springframework.data.repository.query.QueryMethod} a {@link AbstractReactiveGraphRepositoryQuery} can be
 * executed in various flavors.
 *
 * @author lilit gabrielyan
 */
public interface ReactiveGraphQueryExecution {

	Object execute(Query query, Class<?> type);

	@RequiredArgsConstructor
	final class SingleEntityExecution implements ReactiveGraphQueryExecution {

		private final Session session;
		private final ReactiveGraphParameterAccessor accessor;

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
			return Mono.just(ret);
		}
	}

	@RequiredArgsConstructor
	final class CollectionExecution implements ReactiveGraphQueryExecution {

		private final Session session;
		private final ReactiveGraphParameterAccessor accessor;

		@Override
		public Object execute(Query query, Class<?> type) {
			if (query.isFilterQuery()) {
				return Flux.fromIterable(session.loadAll(type, query.getFilters(), accessor.getOgmSort(), accessor.getDepth()));
			} else {
				if (type.getAnnotation(QueryResult.class) != null || Map.class.isAssignableFrom(type)) {
					return Flux.fromIterable(
							session.query(query.getCypherQuery(accessor.getSort()), query.getParameters()).queryResults());
				} else {
					return Flux
							.fromIterable(session.query(type, query.getCypherQuery(accessor.getSort()), query.getParameters()));
				}
			}
		}
	}

	@RequiredArgsConstructor
	final class QueryResultExecution implements ReactiveGraphQueryExecution {

		private final Session session;
		private final ReactiveGraphParameterAccessor accessor;

		@Override
		public Object execute(Query query, Class<?> type) {
			return Mono.just(session.query(query.getCypherQuery(accessor.getSort()), query.getParameters()));
		}
	}

	@RequiredArgsConstructor
	final class CountByExecution implements ReactiveGraphQueryExecution {

		private final Session session;

		@Override
		public Object execute(Query query, Class<?> type) {
			return Mono.just(session.count(type, query.getFilters()));
		}
	}

	@RequiredArgsConstructor
	final class DeleteByExecution implements ReactiveGraphQueryExecution {

		private final Session session;
		private final GraphQueryMethod graphQueryMethod;

		@Override
		public Object execute(Query query, Class<?> type) {
			Class<?> returnType = graphQueryMethod.getReturnedObjectType();

			if (returnType.equals(Long.class)) {
				return Mono.fromRunnable(() -> session.delete(type, query.getFilters(), graphQueryMethod.isCollectionQuery()));
			}
			throw new RuntimeException("Long or Iterable<Long> is required as the return type of a Delete query");
		}
	}

}
