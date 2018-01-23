/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.repository.config;

import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.util.ClassUtils;

/**
 * {@link FactoryBean} to setup {@link Neo4jMappingContext} instances from Spring configuration.
 *
 * @author Mark Angrish
 * @author Nicolas Mervaillie
 */
public class Neo4jMappingContextFactoryBean extends AbstractFactoryBean<Neo4jMappingContext> implements
		ApplicationContextAware {

	private static final Logger LOG = LoggerFactory.getLogger(Neo4jMappingContextFactoryBean.class);
	private static final boolean HAS_ENTITY_INSTANTIATOR_FEATURE = ClassUtils.isPresent("org.neo4j.ogm.session.EntityInstantiator",
			Neo4jMappingContextFactoryBean.class.getClassLoader());
	private ListableBeanFactory beanFactory;

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

		SessionFactory sessionFactory = beanFactory.getBean(SessionFactory.class);
		Neo4jMappingContext context = new Neo4jMappingContext(sessionFactory.metaData());
		context.initialize();

		if (HAS_ENTITY_INSTANTIATOR_FEATURE) {
			ConversionService conversionService = null;
			try {
				conversionService = beanFactory.getBean(ConversionService.class);
			} catch (NoSuchBeanDefinitionException e) {
				LOG.debug("Unable to find a conversion service to use for entity instantiation, using none");
			}
			new DirectFieldAccessor(sessionFactory).setPropertyValue("entityInstantiator",
					new OgmEntityInstantiatorAdapter(context, conversionService));
		}

		return context;
	}

}
