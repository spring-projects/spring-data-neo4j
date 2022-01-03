/*
 * Copyright 2011-2022 the original author or authors.
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

import java.io.Serializable;

import org.apiguardian.api.API;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;

/**
 * Special adapter for Springs {@link org.springframework.beans.factory.FactoryBean} interface to allow easy setup of
 * repository factories via Spring configuration.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @param <T> the type of the repository
 * @param <S> type of the domain class to map
 * @param <ID> identifier type in the domain class
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class ReactiveNeo4jRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

	private ReactiveNeo4jOperations neo4jOperations;

	private Neo4jMappingContext neo4jMappingContext;

	/**
	 * Creates a new {@link TransactionalRepositoryFactoryBeanSupport} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	protected ReactiveNeo4jRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	public void setNeo4jOperations(ReactiveNeo4jOperations neo4jOperations) {
		this.neo4jOperations = neo4jOperations;
	}

	public void setNeo4jMappingContext(Neo4jMappingContext neo4jMappingContext) {
		super.setMappingContext(neo4jMappingContext);
		this.neo4jMappingContext = neo4jMappingContext;
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		return new ReactiveNeo4jRepositoryFactory(neo4jOperations, neo4jMappingContext);
	}
}
