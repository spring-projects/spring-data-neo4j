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
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Oliver Gierke
 * @author Nicolas Mervaillie
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class GraphQueryLookupStrategy implements QueryLookupStrategy {

	private final Session session;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private final MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext;

	/**
	 * @param session
	 * @param evaluationContextProvider
	 * @deprecated since 5.1.0, use
	 *             {@link GraphQueryLookupStrategy#GraphQueryLookupStrategy(Session, QueryMethodEvaluationContextProvider, MappingContext)}
	 *             instead and provide the mapping context.
	 */
	@Deprecated
	public GraphQueryLookupStrategy(Session session, QueryMethodEvaluationContextProvider evaluationContextProvider) {
		this(session, evaluationContextProvider, null);
	}

	public GraphQueryLookupStrategy(Session session, QueryMethodEvaluationContextProvider evaluationContextProvider,
			@Nullable MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {
		this.session = session;
		this.evaluationContextProvider = evaluationContextProvider;
		this.mappingContext = mappingContext;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
	 */
	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			NamedQueries namedQueries) {

		GraphQueryMethod queryMethod = new GraphQueryMethod(method, metadata, factory);
		queryMethod.setMappingContext(this.mappingContext);
		String namedQueryName = queryMethod.getNamedQueryName();

		if (namedQueries.hasQuery(namedQueryName)) {
			String cypherQuery = namedQueries.getQuery(namedQueryName);
			return new NamedGraphRepositoryQuery(queryMethod, session, cypherQuery, evaluationContextProvider);
		} else if (queryMethod.hasAnnotatedQuery()) {
			return new GraphRepositoryQuery(queryMethod, session, evaluationContextProvider);
		} else {
			return new PartTreeNeo4jQuery(queryMethod, session);
		}
	}
}
