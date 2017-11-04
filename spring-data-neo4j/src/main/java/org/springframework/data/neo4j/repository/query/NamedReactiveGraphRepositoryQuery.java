package org.springframework.data.neo4j.repository.query;

import org.neo4j.ogm.session.Session;

/**
 * Specialisation of {@link ReactiveGraphRepositoryQuery} that creates queries from named queries defined in
 * {@code META-INFO/neo4j-named-queries.properties}.
 *
 * @author lilit gabrielyan
 */
public class NamedReactiveGraphRepositoryQuery extends ReactiveGraphRepositoryQuery {

	private final String cypherQuery;

	NamedReactiveGraphRepositoryQuery(ReactiveGraphQueryMethod graphQueryMethod, Session session, String cypherQuery) {
		super(graphQueryMethod, session);
		this.cypherQuery = cypherQuery;
	}

	@Override
	protected Query getQuery(Object[] parameters) {
		return new Query(cypherQuery, resolveParams(parameters));
	}
}
