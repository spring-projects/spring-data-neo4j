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
package org.springframework.data.neo4j.core.mapping.callback;

import org.springframework.core.Ordered;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;

/**
 * Callback to increment the value of the version property for a given entity.
 *
 * @author Gerrit Meier
 * @since 6.0.2
 */
final class OptimisticLockingBeforeBindCallback implements BeforeBindCallback<Object>, Ordered {

	private final OptimisticLockingSupport optimisticLocking;

	OptimisticLockingBeforeBindCallback(Neo4jMappingContext neo4jMappingContext) {
		this.optimisticLocking = new OptimisticLockingSupport(neo4jMappingContext);
	}

	@Override
	public Object onBeforeBind(Object entity) {
		return optimisticLocking.getAndIncrementVersionPropertyIfNecessary(entity);
	}

	@Override
	public int getOrder() {
		return AuditingBeforeBindCallback.NEO4J_AUDITING_ORDER + 11;
	}
}
