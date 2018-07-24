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

package org.springframework.data.neo4j.repository.support;

import java.util.Optional;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.query.GraphQueryLookupStrategy;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.util.Assert;

/**
 * Neo4j OGM specific generic repository factory.
 *
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 */
public class Neo4jRepositoryFactory extends RepositoryFactorySupport {

	private final Session session;

	public Neo4jRepositoryFactory(Session session) {
		Assert.notNull(session, "Session must not be null!");
		this.session = session;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		super.setBeanClassLoader(classLoader);
	}

	@Override
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> type) {
		Assert.notNull(type, "Domain class must not be null!");
		Assert.notNull(session, "Session must not be null!");
		return new GraphEntityInformation(((Neo4jSession) session).metaData(), type);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation information) {
		return getTargetRepositoryViaReflection(information, information.getDomainType(), session);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
		return SimpleNeo4jRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of(new GraphQueryLookupStrategy(session, evaluationContextProvider));
	}
}
