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
