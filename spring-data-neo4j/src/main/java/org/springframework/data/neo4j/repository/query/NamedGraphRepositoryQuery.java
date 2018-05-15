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

import org.neo4j.ogm.session.Session;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

/**
 * Specialisation of {@link GraphRepositoryQuery} that creates queries from named queries defined in
 * {@code META-INFO/neo4j-named-queries.properties}.
 *
 * @author Gerrit Meier
 */
public class NamedGraphRepositoryQuery extends GraphRepositoryQuery {

	private final String cypherQuery;

	NamedGraphRepositoryQuery(GraphQueryMethod graphQueryMethod, Session session, String cypherQuery,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		super(graphQueryMethod, session, evaluationContextProvider);
		this.cypherQuery = cypherQuery;
	}

	@Override
	protected Query getQuery(Object[] parameters) {
		return new Query(cypherQuery, resolveParams(getGraphQueryMethod().getParameters(), parameters));
	}

}
