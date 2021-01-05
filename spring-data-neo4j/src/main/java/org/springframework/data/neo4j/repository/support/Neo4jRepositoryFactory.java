/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.repository.support;

import java.util.Optional;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.repository.query.GraphQueryLookupStrategy;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Neo4j OGM specific generic repository factory.
 *
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Michael J. Simons
 */
public class Neo4jRepositoryFactory extends RepositoryFactorySupport {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jRepositoryFactory.class);

	private final Session session;

	private @Nullable final MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext;

	/**
	 * @param session
	 * @deprecated since 5.1.0, use {@link Neo4jRepositoryFactory#Neo4jRepositoryFactory(Session, MappingContext)} instead
	 *             and provide the mapping context.
	 */
	@Deprecated
	public Neo4jRepositoryFactory(Session session) {
		this(session, null);
	}

	public Neo4jRepositoryFactory(Session session, MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {
		Assert.notNull(session, "Session must not be null!");

		this.session = session;
		if (mappingContext != null) {
			this.mappingContext = mappingContext;
		} else if(session instanceof Neo4jSession) {
			logger.debug("Creating a new mapping context");
			this.mappingContext = new Neo4jMappingContext(((Neo4jSession) session).metaData());
			((Neo4jMappingContext) this.mappingContext).initialize();
		} else {
			logger.warn("No mapping context present, some operations won't support persistence constructors");
			this.mappingContext = null;
		}
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
		return Optional.of(new GraphQueryLookupStrategy(session, evaluationContextProvider, this.mappingContext));
	}
}
