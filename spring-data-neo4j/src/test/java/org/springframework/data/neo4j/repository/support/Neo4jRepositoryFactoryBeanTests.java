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

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.data.neo4j.mapping.MetaDataProvider;
import org.springframework.data.neo4j.repository.sample.repo.ContactRepository;

/**
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Michael J. Simons
 */
@RunWith(MockitoJUnitRunner.class)
public class Neo4jRepositoryFactoryBeanTests {

	Neo4jRepositoryFactoryBean factoryBean;

	@Mock ListableBeanFactory beanFactory;

	@Mock MappingContext context;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		Session session = mock(Session.class, withSettings().extraInterfaces(MetaDataProvider.class));

		// Setup standard factory configuration
		factoryBean = new Neo4jRepositoryFactoryBean(ContactRepository.class);
		factoryBean.setSession(session);
		factoryBean.setMappingContext(context);
	}

	/**
	 * Assert that the instance created for the standard configuration is a valid {@code UserRepository}.
	 */
	@Test
	public void setsUpBasicInstanceCorrectly() {

		factoryBean.setBeanFactory(beanFactory);
		factoryBean.afterPropertiesSet();

		assertThat(factoryBean.getObject()).isNotNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void requiresListableBeanFactory() {

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
