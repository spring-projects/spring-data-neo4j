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
package org.springframework.data.neo4j.repository.config;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} to setup {@link Neo4jMappingContext} instances from Spring configuration.
 *
 * @author Mark Angrish
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
public class Neo4jMappingContextFactoryBean extends AbstractFactoryBean<Neo4jMappingContext>
		implements ApplicationContextAware {

	private final String sessionFactoryBeanName;

	private ListableBeanFactory beanFactory;

	/**
	 * Uses the default session factory name
	 * {@link Neo4jRepositoryConfigurationExtension#DEFAULT_SESSION_FACTORY_BEAN_NAME} for finding the session factory
	 * passed to the {@link Neo4jMappingContext}.
	 *
	 * @deprecated since 5.1.0, use {@link #Neo4jMappingContextFactoryBean(String)} instead
	 */
	@Deprecated
	public Neo4jMappingContextFactoryBean() {
		this(Neo4jRepositoryConfigurationExtension.DEFAULT_SESSION_FACTORY_BEAN_NAME);
	}

	/**
	 * Configures the mapping context with a named {@link SessionFactory}.
	 *
	 * @param sessionFactoryBeanName must not be {@literal null} or empty.
	 * @since 5.1.0
	 */
	public Neo4jMappingContextFactoryBean(String sessionFactoryBeanName) {

		Assert.hasText(sessionFactoryBeanName, "SessionFactoryBeanName must not be null or empty!");

		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return Neo4jMappingContext.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
	 */
	@Override
	protected Neo4jMappingContext createInstance() {

		SessionFactory sessionFactory = beanFactory.getBean(this.sessionFactoryBeanName, SessionFactory.class);
		Neo4jMappingContext context = new Neo4jMappingContext(sessionFactory.metaData());

		context.initialize();

		return context;
	}

}
