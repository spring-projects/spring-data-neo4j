/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.support;

import java.io.Serializable;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * {@link org.springframework.beans.factory.FactoryBean} to create
 * {@link org.springframework.data.neo4j.repository.ReactiveNeo4jRepository} instances.
 *
 * @author lilit gabrielyan
 * @since 5.0
 * @see org.springframework.data.repository.reactive.ReactiveSortingRepository
 * @see org.springframework.data.repository.reactive.RxJava2SortingRepository
 */
public class ReactiveNeo4jRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends Neo4jRepositoryFactoryBean<T, S, ID> {

	private Session session;

	/**
	 * Creates a new {@link ReactiveNeo4jRepositoryFactoryBean} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public ReactiveNeo4jRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	@Autowired
	public void setSession(Session session) {
		super.setSession(session);
		this.session = session;
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		return createRepositoryFactory(session);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.data.repository.support.RepositoryFactoryBeanSupport
	 * #createRepositoryFactory()
	 */
	@Override
	protected final RepositoryFactorySupport createRepositoryFactory(Session session) {

		return new ReactiveNeo4jRepositoryFactory(session);
	}

}
