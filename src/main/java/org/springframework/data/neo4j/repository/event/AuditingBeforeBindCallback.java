/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.repository.event;

import org.apiguardian.api.API;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.Ordered;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.mapping.callback.EntityCallback;
import org.springframework.util.Assert;

/**
 * {@link EntityCallback} to populate auditing related fields on an entity about to be bound to a record.
 *
 * @author Michael J. Simons
 * @soundtrack Iron Maiden - Iron Maiden
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class AuditingBeforeBindCallback implements BeforeBindCallback<Object>, Ordered {

	public static final int NEO4J_AUDITING_ORDER = 100;

	private final ObjectFactory<IsNewAwareAuditingHandler> auditingHandlerFactory;

	/**
	 * Creates a new {@link AuditingBeforeBindCallback} using the given {@link AuditingHandler} provided by the given
	 * {@link ObjectFactory}.
	 *
	 * @param auditingHandlerFactory must not be {@literal null}.
	 */
	public AuditingBeforeBindCallback(ObjectFactory<IsNewAwareAuditingHandler> auditingHandlerFactory) {

		Assert.notNull(auditingHandlerFactory, "IsNewAwareAuditingHandler must not be null!");
		this.auditingHandlerFactory = auditingHandlerFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.neo4j.repository.event.BeforeBindCallback#onBeforeBind(java.lang.Object)
	 */
	@Override
	public Object onBeforeBind(Object entity) {
		return auditingHandlerFactory.getObject().markAudited(entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	@Override
	public int getOrder() {
		return NEO4J_AUDITING_ORDER;
	}
}
