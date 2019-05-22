/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.query;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

import org.apiguardian.api.API;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Lookup strategy for queries. This is the internal api of the {@code query package}.
 *
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
@RequiredArgsConstructor
public final class ReactiveNeo4jQueryLookupStrategy implements QueryLookupStrategy {

	private final ReactiveNeo4jClient neo4jClient;
	private final Neo4jMappingContext mappingContext;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
	 */
	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
		NamedQueries namedQueries) {

		Neo4jQueryMethod queryMethod = new Neo4jQueryMethod(method, metadata, factory);
		String namedQueryName = queryMethod.getNamedQueryName();

		if (namedQueries.hasQuery(namedQueryName)) {
			return ReactiveStringBasedNeo4jQuery.create(neo4jClient, mappingContext, evaluationContextProvider, queryMethod,
				namedQueries.getQuery(namedQueryName));
		} else if (queryMethod.hasQueryAnnotation()) {
			return ReactiveStringBasedNeo4jQuery.create(neo4jClient, mappingContext, evaluationContextProvider, queryMethod);
		} else {
			return new ReactivePartTreeNeo4jQuery(neo4jClient, mappingContext, queryMethod);
		}
	}
}
