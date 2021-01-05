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
import org.neo4j.ogm.session.event.Event;
import org.neo4j.ogm.session.event.EventListener;
import org.neo4j.ogm.session.event.EventListenerAdapter;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.repository.support.Neo4jAuditingBeanPostProcessor;
import org.springframework.util.Assert;

/**
 * @author Frantisek Hartman
 */
public class Neo4jAuditingEventListener extends EventListenerAdapter implements EventListener {

	private final ObjectFactory<? extends IsNewAwareAuditingHandler> auditingHandlerFactory;

	/**
	 * Creates a new {@link Neo4jAuditingEventListener} using the given {@link MappingContext} and {@link AuditingHandler}
	 * provided by the given {@link ObjectFactory}. This constructor does an additional registration to the
	 * {@link SessionFactory}. Therefore the {@link SessionFactory} must already be instantiated.
	 *
	 * @deprecated this constructor could create a cyclic dependency to the session factory if this class gets registered
	 *             as a bean in the context.
	 * @param auditingHandlerFactory must not be {@literal null}.
	 */
	@Deprecated
	public Neo4jAuditingEventListener(ObjectFactory<? extends IsNewAwareAuditingHandler> auditingHandlerFactory,
			SessionFactory sessionFactory) {

		Assert.notNull(auditingHandlerFactory, "IsNewAwareAuditingHandler must not be null!");
		this.auditingHandlerFactory = auditingHandlerFactory;

		sessionFactory.register(this);
	}

	/**
	 * Constructor used for creating an instance in the {@link Neo4jAuditingRegistrar} to get registered in the session
	 * "manually". The registration is done within the {@link Neo4jAuditingBeanPostProcessor}.
	 *
	 * @param auditingHandlerFactory {@link AuditingHandler} to hook into the {@code preSave} phase for auditing.
	 */
	public Neo4jAuditingEventListener(ObjectFactory<? extends IsNewAwareAuditingHandler> auditingHandlerFactory) {
		Assert.notNull(auditingHandlerFactory, "IsNewAwareAuditingHandler must not be null!");
		this.auditingHandlerFactory = auditingHandlerFactory;
	}

	@Override
	public void onPreSave(Event event) {
		Object object = event.getObject();
		if (object != null) {
			auditingHandlerFactory.getObject().markAudited(object);
		}
	}

}
