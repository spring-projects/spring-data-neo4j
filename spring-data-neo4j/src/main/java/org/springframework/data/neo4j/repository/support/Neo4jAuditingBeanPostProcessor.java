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
package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.neo4j.repository.config.Neo4jAuditingEventListener;
import org.springframework.util.Assert;

public class Neo4jAuditingBeanPostProcessor implements BeanPostProcessor {

	private final ObjectFactory<IsNewAwareAuditingHandler> isNewAwareHandler;

	public Neo4jAuditingBeanPostProcessor(ObjectFactory<IsNewAwareAuditingHandler> isNewAwareHandler) {

		Assert.notNull(isNewAwareHandler, "IsNewAwareHandler must not be null!");

		this.isNewAwareHandler = isNewAwareHandler;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		if (!SessionFactory.class.isInstance(bean)) {
			return bean;
		}

		SessionFactory sessionFactory = (SessionFactory) bean;
		sessionFactory.register(new Neo4jAuditingEventListener(isNewAwareHandler));

		return sessionFactory;
	}
}
