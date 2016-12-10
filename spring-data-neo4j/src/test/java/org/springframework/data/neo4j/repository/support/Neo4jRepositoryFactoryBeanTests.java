/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Persistable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * @author Mark Angrish
 */
@RunWith(MockitoJUnitRunner.class)
public class Neo4jRepositoryFactoryBeanTests {

	Neo4jRepositoryFactoryBean<SimpleSampleRepository, User, Long> factoryBean;

	@Mock
	Session session;
	@Mock
	RepositoryFactorySupport factory;
	@Mock
	ListableBeanFactory beanFactory;
	@Mock
	PersistenceExceptionTranslator translator;
	@Mock
	Repository<?, ?> repository;
	@Mock
	MetaData metaData;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		Map<String, PersistenceExceptionTranslator> beans = new HashMap<>();
		beans.put("foo", translator);
		when(beanFactory.getBeansOfType(eq(PersistenceExceptionTranslator.class), anyBoolean(), anyBoolean())).thenReturn(
				beans);
		when(factory.getRepository(any(Class.class), any(Object.class))).thenReturn(repository);

		// Setup standard factory configuration
		factoryBean = new DummyNeo4jRepositoryFactoryBean(SimpleSampleRepository.class);
		factoryBean.setSession(session);
	}

	/**
	 * Assert that the instance created for the standard configuration is a valid {@code UserRepository}.
	 *
	 * @throws Exception
	 */
	@Test
	public void setsUpBasicInstanceCorrectly() throws Exception {

		factoryBean.setBeanFactory(beanFactory);
		factoryBean.afterPropertiesSet();

		assertNotNull(factoryBean.getObject());
	}

	@Test(expected = IllegalArgumentException.class)
	public void requiresListableBeanFactory() throws Exception {

		factoryBean.setBeanFactory(mock(BeanFactory.class));
	}

	/**
	 * Assert that the factory rejects calls to {@code Neo4jRepositoryFactoryBean#setRepositoryInterface(Class)} with
	 * {@literal null} or any other parameter instance not implementing {@code Repository}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsNullRepositoryInterface() {

		factoryBean = new DummyNeo4jRepositoryFactoryBean(null);
	}

	private class DummyNeo4jRepositoryFactoryBean<T extends Neo4jRepository<S, ID>, S, ID extends Serializable> extends
			Neo4jRepositoryFactoryBean<T, S, ID> {

		/**
		 * @param repositoryInterface
		 */
		public DummyNeo4jRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
			super(repositoryInterface);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean
		 * #createRepositoryFactory()
		 */
		@Override
		protected RepositoryFactorySupport doCreateRepositoryFactory() {

			return factory;
		}
	}

	private interface SimpleSampleRepository extends Neo4jRepository<User, Long> {

	}

	@SuppressWarnings("serial")
	private static abstract class User implements Persistable<Long> {

	}
}
