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

import java.lang.reflect.Method;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.query.derived.DerivedGraphRepositoryQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Oliver Gierke
 * @author Nicolas Mervaillie
 * @author Gerrit Meier
 */
public class GraphQueryLookupStrategy implements QueryLookupStrategy {

	private final Session session;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	public GraphQueryLookupStrategy(Session session, QueryMethodEvaluationContextProvider evaluationContextProvider) {
		this.session = session;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
	 */
	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			NamedQueries namedQueries) {

		GraphQueryMethod queryMethod = new GraphQueryMethod(method, metadata, factory);
		String namedQueryName = queryMethod.getNamedQueryName();

		if (namedQueries.hasQuery(namedQueryName)) {
			String cypherQuery = namedQueries.getQuery(namedQueryName);
			return new NamedGraphRepositoryQuery(queryMethod, session, cypherQuery, evaluationContextProvider);
		} else if (queryMethod.hasAnnotatedQuery()) {
			return new GraphRepositoryQuery(queryMethod, session, evaluationContextProvider);
		} else {
			return new DerivedGraphRepositoryQuery(queryMethod, session);
		}
	}
}
