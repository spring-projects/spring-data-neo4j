/*
 * Copyright 2011-2020 the original author or authors.
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

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.session.Session;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.mapping.MetaDataProvider;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Oliver Gierke
 * @author Nicolas Mervaillie
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class GraphQueryLookupStrategy implements QueryLookupStrategy {

	private final MetaData metaData;
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

		this.metaData = getMetaData(session);
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
			return new NamedGraphRepositoryQuery(queryMethod, metaData, session, cypherQuery, evaluationContextProvider);
		} else if (queryMethod.hasAnnotatedQuery()) {
			return new GraphRepositoryQuery(queryMethod, metaData, session, evaluationContextProvider);
		} else {
			return new PartTreeNeo4jQuery(queryMethod, metaData, session);
		}
	}

	private static MetaData getMetaData(Session session) {

		// This is the case in most standard setups: The SharedSessionCreator adds this MetaDataProvider interface
		if (session instanceof MetaDataProvider) {
			return ((MetaDataProvider) session).getMetaData();
		}

		// That is usually the case in a programmatic setup of repositories or in the case of CDI environment.
		// Note that in this case the meta-data is retrieved from the session, not from the session-factory.
		Method getMetaData = ReflectionUtils.findMethod(session.getClass(), "metaData");
		if (getMetaData == null) {
			throw new IllegalStateException("Could not retrieve Neo4j-OGM MetaData from session.");
		} else {
			return (MetaData) ReflectionUtils.invokeMethod(getMetaData, session);
		}
	}

}
