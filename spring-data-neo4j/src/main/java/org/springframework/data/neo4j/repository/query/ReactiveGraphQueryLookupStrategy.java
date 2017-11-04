package org.springframework.data.neo4j.repository.query;

import lombok.RequiredArgsConstructor;
import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.query.derived.ReactiveDerivedGraphRepositoryQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import java.lang.reflect.Method;

/**
 * {@link QueryLookupStrategy} to create {@link ReactiveGraphRepositoryQuery} instances.
 *
 * @author lilit gabrielyan
 */
@RequiredArgsConstructor
public class ReactiveGraphQueryLookupStrategy implements QueryLookupStrategy {

	private final Session session;

	/*
	* (non-Javadoc)
	* @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
	*/
	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			NamedQueries namedQueries) {

		ReactiveGraphQueryMethod queryMethod = new ReactiveGraphQueryMethod(method, metadata, factory);
		String namedQueryName = queryMethod.getNamedQueryName();

		if (namedQueries.hasQuery(namedQueryName)) {
			String cypherQuery = namedQueries.getQuery(namedQueryName);
			return new NamedReactiveGraphRepositoryQuery(queryMethod, session, cypherQuery);
		} else if (queryMethod.hasAnnotatedQuery()) {
			return new ReactiveGraphRepositoryQuery(queryMethod, session);
		} else {
			return new ReactiveDerivedGraphRepositoryQuery(queryMethod, session);
		}
	}
}
