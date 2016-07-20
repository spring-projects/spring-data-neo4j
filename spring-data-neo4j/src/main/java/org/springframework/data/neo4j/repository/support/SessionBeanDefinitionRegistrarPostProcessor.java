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

import static org.springframework.beans.factory.BeanFactoryUtils.*;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * {@link BeanFactoryPostProcessor} to register a {@link SessionFactoryBeanDefinition} for the
 * {@link SessionFactory} bean definition found in the application context to enable autowiring
 * {@link Session} instances into constructor arguments.
 *
 * @author Mark Angrish
 */
public class SessionBeanDefinitionRegistrarPostProcessor implements BeanFactoryPostProcessor {

	private static String getSessionFactoryBeanRef(ConfigurableListableBeanFactory beanFactory) {

		return beanFactory.containsBeanDefinition("sessionFactory") ? "sessionFactory" : "getSessionFactory";
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		SessionFactoryBeanDefinition sfbd = new SessionFactoryBeanDefinition(transformedBeanName(getSessionFactoryBeanRef(beanFactory)), beanFactory);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition("org.springframework.data.neo4j.transaction.SharedSessionCreator");
		builder.setFactoryMethod("createSharedSession");
		builder.addConstructorArgReference(sfbd.getBeanName());

		AbstractBeanDefinition emBeanDefinition = builder.getRawBeanDefinition();

		emBeanDefinition.setScope(sfbd.getBeanDefinition().getScope());
		emBeanDefinition.setSource(sfbd.getBeanDefinition().getSource());
		emBeanDefinition.setAutowireCandidate(true);
		BeanDefinitionReaderUtils.registerWithGeneratedName(emBeanDefinition,
				(BeanDefinitionRegistry) sfbd.getBeanFactory());
	}

	private static class SessionFactoryBeanDefinition {

		private final String beanName;
		private final ConfigurableListableBeanFactory beanFactory;

		SessionFactoryBeanDefinition(String beanName, ConfigurableListableBeanFactory beanFactory) {

			this.beanName = beanName;
			this.beanFactory = beanFactory;
		}

		String getBeanName() {
			return beanName;
		}

		BeanFactory getBeanFactory() {
			return beanFactory;
		}

		BeanDefinition getBeanDefinition() {
			return beanFactory.getBeanDefinition(beanName);
		}
	}
}
