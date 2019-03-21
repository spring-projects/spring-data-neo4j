/*
 * Copyright 2011-2019 the original author or authors.
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
import org.neo4j.ogm.session.event.EventListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.neo4j.repository.config.Neo4jAuditingEventListener;

public class Neo4jAuditingBeanFactoryPostProcessor implements BeanPostProcessor {

	private final SessionFactory sessionFactory;

	@Autowired
	public Neo4jAuditingBeanFactoryPostProcessor(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Neo4jAuditingEventListener) {
			EventListener auditingEventListener = (EventListener) bean;

			// we need to de-register first to be sure we just register once.
			// Background: SpringBoot auto configuration does register all event listener in the context.
			sessionFactory.deregister(auditingEventListener);
			sessionFactory.register(auditingEventListener);
		}
		return bean;
	}
}
