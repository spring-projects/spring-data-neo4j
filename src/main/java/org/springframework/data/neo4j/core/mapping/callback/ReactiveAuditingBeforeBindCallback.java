/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping.callback;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.Ordered;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.ReactiveIsNewAwareAuditingHandler;
import org.springframework.data.mapping.callback.EntityCallback;
import org.springframework.util.Assert;

/**
 * Reactive {@link EntityCallback} to populate auditing related fields on an entity about to be bound to a record.
 *
 * @author Michael J. Simons
 * @soundtrack Iron Maiden - The Number Of The Beast
 * @since 6.0.2
 */
@API(status = STABLE, since = "6.0.2")
public final class ReactiveAuditingBeforeBindCallback implements ReactiveBeforeBindCallback<Object>, Ordered {

	public static final int NEO4J_REACTIVE_AUDITING_ORDER = 100;

	private final ObjectFactory<ReactiveIsNewAwareAuditingHandler> auditingHandlerFactory;

	/**
	 * Creates a new {@link ReactiveAuditingBeforeBindCallback} using the {@link AuditingHandler} provided by the given
	 * {@link ObjectFactory}.
	 *
	 * @param auditingHandlerFactory must not be {@literal null}.
	 */
	public ReactiveAuditingBeforeBindCallback(ObjectFactory<ReactiveIsNewAwareAuditingHandler> auditingHandlerFactory) {

		Assert.notNull(auditingHandlerFactory, "IsNewAwareAuditingHandler must not be null");
		this.auditingHandlerFactory = auditingHandlerFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.neo4j.repository.event.ReactiveBeforeBindCallback#onBeforeBind(java.lang.Object)
	 */
	@Override
	public Publisher<Object> onBeforeBind(Object entity) {

		return auditingHandlerFactory.getObject().markAudited(entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	@Override
	public int getOrder() {
		return NEO4J_REACTIVE_AUDITING_ORDER;
	}
}
