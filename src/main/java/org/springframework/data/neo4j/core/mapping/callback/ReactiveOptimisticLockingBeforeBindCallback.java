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
package org.springframework.data.neo4j.core.mapping.callback;

import reactor.core.publisher.Mono;

import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;

/**
 * Callback to increment the value of the version property for a given entity.
 *
 * @author Gerrit Meier
 * @since 6.0
 */
final class ReactiveOptimisticLockingBeforeBindCallback implements ReactiveBeforeBindCallback<Object>, Ordered {

	private final Neo4jMappingContext neo4jMappingContext;

	ReactiveOptimisticLockingBeforeBindCallback(Neo4jMappingContext neo4jMappingContext) {
		this.neo4jMappingContext = neo4jMappingContext;
	}

	@Override
	public Publisher<Object> onBeforeBind(Object entity) {

		return Mono.fromSupplier(() -> {
			Neo4jPersistentEntity<?> neo4jPersistentEntity = (Neo4jPersistentEntity<?>) neo4jMappingContext
					.getRequiredNodeDescription(entity.getClass());

			if (neo4jPersistentEntity.hasVersionProperty()) {
				PersistentPropertyAccessor<Object> propertyAccessor = neo4jPersistentEntity.getPropertyAccessor(entity);
				Neo4jPersistentProperty versionProperty = neo4jPersistentEntity.getRequiredVersionProperty();

				if (!Long.class.isAssignableFrom(versionProperty.getType())) {
					return entity;
				}

				Long versionPropertyValue = (Long) propertyAccessor.getProperty(versionProperty);

				long newVersionValue = 0;
				if (versionPropertyValue != null) {
					newVersionValue = versionPropertyValue + 1;
				}

				propertyAccessor.setProperty(versionProperty, newVersionValue);
			}
			return entity;
		});
	}

	@Override
	public int getOrder() {
		return ReactiveAuditingBeforeBindCallback.NEO4J_REACTIVE_AUDITING_ORDER + 11;
	}
}
