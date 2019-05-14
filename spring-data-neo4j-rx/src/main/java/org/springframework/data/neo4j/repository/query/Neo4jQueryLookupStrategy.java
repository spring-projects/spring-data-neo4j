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
import java.util.Optional;

import org.apiguardian.api.API;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.NodeManager;
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
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
@RequiredArgsConstructor
public final class Neo4jQueryLookupStrategy implements QueryLookupStrategy {

	private final NodeManager nodeManager;
	private final Neo4jMappingContext mappingContext;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
	 */
	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
		NamedQueries namedQueries) {

		Neo4jQueryMethod queryMethod = new Neo4jQueryMethod(method, metadata, factory);

		Optional<Query> optionalQueryAnnotation = getQueryAnnotationOf(method);
		if (optionalQueryAnnotation.isPresent()) {
			return new StringBasedNeo4jQuery(nodeManager, mappingContext, queryMethod,
				getCypherQuery(optionalQueryAnnotation), optionalQueryAnnotation);
		}

		return new PartTreeNeo4jQuery(nodeManager, mappingContext, queryMethod);
	}

	/**
	 * @return the {@link Query} annotation that is applied to the method or an empty {@link Optional} if none available.
	 */
	static Optional<Query> getQueryAnnotationOf(Method method) {
		return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, Query.class));
	}

	static String getCypherQuery(Optional<Query> optionalQueryAnnotation) {
		return optionalQueryAnnotation.map(Query::value).filter(s -> !s.isEmpty())
			.orElseThrow(() -> new MappingException("Expected @Query annotation to have a value, but it did not."));
	}
}
