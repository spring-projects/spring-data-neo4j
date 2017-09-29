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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.repository.ContactRepository;

/**
 * @author Mark Angrish
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class Neo4jRepositoryFactoryBeanTests {

	Neo4jRepositoryFactoryBean factoryBean;

	@Mock Session session;

	@Mock ListableBeanFactory beanFactory;

	@Mock MappingContext context;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		// Setup standard factory configuration
		factoryBean = new Neo4jRepositoryFactoryBean(ContactRepository.class);
		factoryBean.setSession(session);
		factoryBean.setMappingContext(context);
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

		factoryBean = new Neo4jRepositoryFactoryBean(null);
	}
}
