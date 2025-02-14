/*
 * Copyright 2011-2025 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.List;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Lookup strategy for queries. This is the internal api of the {@code query package}.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class Neo4jQueryLookupStrategy implements QueryLookupStrategy {

	private final Neo4jMappingContext mappingContext;
	private final Neo4jOperations neo4jOperations;
	private final ValueExpressionDelegate delegate;
	private final Configuration configuration;
	private final List<CustomStatementKreator> kreatorBeans;

	public Neo4jQueryLookupStrategy(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
									ValueExpressionDelegate delegate, Configuration configuration, List<CustomStatementKreator> kreatorBeans) {
		this.neo4jOperations = neo4jOperations;
		this.mappingContext = mappingContext;
		this.delegate = delegate;
		this.configuration = configuration;
		this.kreatorBeans = kreatorBeans;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
	 */
	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			NamedQueries namedQueries) {

		Neo4jQueryMethod queryMethod = new Neo4jQueryMethod(method, metadata, factory);
		String namedQueryName = queryMethod.getNamedQueryName();

		if (namedQueries.hasQuery(namedQueryName)) {
			return StringBasedNeo4jQuery.create(neo4jOperations, mappingContext, delegate, queryMethod,
					namedQueries.getQuery(namedQueryName), factory);
		} else if (queryMethod.hasQueryAnnotation()) {
			return StringBasedNeo4jQuery.create(neo4jOperations, mappingContext, delegate, queryMethod,
					factory);
		} else if (queryMethod.isCypherBasedProjection()) {
			return CypherdslBasedQuery.create(neo4jOperations, mappingContext, queryMethod, factory, Renderer.getRenderer(configuration)::render);
		} else {
			return PartTreeNeo4jQuery.create(neo4jOperations, mappingContext, queryMethod, factory, kreatorBeans);
		}
	}
}
