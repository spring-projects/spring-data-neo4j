package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.query.ReactiveGraphQueryLookupStrategy;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.util.Assert;

import java.util.Optional;

public class ReactiveNeo4jRepositoryFactory extends ReactiveRepositoryFactorySupport {

	private final Session session;

	/**
	 * Creates a new {@link ReactiveNeo4jRepositoryFactory} with the given {@link Session}.
	 *
	 * @param session must not be {@literal null}.
	 */
	public ReactiveNeo4jRepositoryFactory(Session session) {
		Assert.notNull(session, "Session must not be null!");
		this.session = session;
	}

	/*
	   * (non-Javadoc)
	   * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	   */

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleReactiveNeo4jRepository.class;
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
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
			EvaluationContextProvider evaluationContextProvider) {
		return Optional.of(new ReactiveGraphQueryLookupStrategy(session));
	}
}
