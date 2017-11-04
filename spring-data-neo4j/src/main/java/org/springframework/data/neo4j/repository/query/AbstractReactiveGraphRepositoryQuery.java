package org.springframework.data.neo4j.repository.query;

import lombok.NonNull;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

import java.util.EmptyStackException;

/**
 * Base class for @link {@link RepositoryQuery}s.
 *
 * @author lilit gabrielyan
 */
public abstract class AbstractReactiveGraphRepositoryQuery implements RepositoryQuery {

	private final ReactiveGraphQueryMethod method;
	private final Session session;

	protected AbstractReactiveGraphRepositoryQuery(ReactiveGraphQueryMethod method, Session session) {

		this.method = method;
		this.session = session;
	}

	protected abstract Query getQuery(Object[] parameters);

	protected abstract Object doExecute(Query params, Object[] parameters);

	@Override
	public Object execute(@Nullable Object[] parameters) {

		Query query;
		try {
			query = getQuery(parameters);
		} catch (EmptyStackException e) {
			throw new IllegalArgumentException("Not enough arguments for query " + getQueryMethod().getName());
		}

		return doExecute(query, parameters);
	}

	@Override
	public ReactiveGraphQueryMethod getQueryMethod() {
		return method;
	}

	/**
	 * Returns the execution instance to use.
	 *
	 * @param accessor must not be {@literal null}.
	 */
	protected ReactiveGraphQueryExecution getExecution(ReactiveGraphParameterAccessor accessor) {

		if (isCountQuery()) {
			return new ReactiveGraphQueryExecution.CountByExecution(session);
		}
		if (isDeleteQuery()) {
			return new ReactiveGraphQueryExecution.DeleteByExecution(session, method);
		}
		if (returnsOgmSpecificType()) {
			return new ReactiveGraphQueryExecution.QueryResultExecution(session, accessor);
		}
		if (method.isCollectionQuery()) {
			return new ReactiveGraphQueryExecution.CollectionExecution(session, accessor);
		}
		return new ReactiveGraphQueryExecution.SingleEntityExecution(session, accessor);
	}

	/**
	 * Does the query returns an OGM specific object type that should get a special processing ?
	 *
	 * @return true is that's the case
	 */
	private boolean returnsOgmSpecificType() {
		TypeInformation<?> returnType = method.getReturnType();
		TypeInformation<?> componentType = returnType.getComponentType();
		return componentType != null && (QueryStatistics.class.isAssignableFrom(componentType.getType())
				|| Result.class.isAssignableFrom(componentType.getType()));
	}

	/**
	 * @return Returns whether the query should get a count projection applied.
	 */
	protected abstract boolean isCountQuery();

	/**
	 * @return Return weather the query should delete matching documents.
	 */
	protected abstract boolean isDeleteQuery();
}
