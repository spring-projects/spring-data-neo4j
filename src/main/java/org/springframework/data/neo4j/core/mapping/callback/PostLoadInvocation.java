/*
 * Copyright 2011-present the original author or authors.
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

import org.neo4j.driver.types.MapAccessor;

import org.springframework.core.Ordered;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;

/**
 * Triggers {@link Neo4jMappingContext#invokePostLoad(Neo4jPersistentEntity, Object)} via
 * the {@link AfterConvertCallback} mechanism.
 *
 * @author Michael J. Simons
 */
final class PostLoadInvocation implements AfterConvertCallback<Object>, Ordered {

	private final Neo4jMappingContext neo4jMappingContext;

	PostLoadInvocation(Neo4jMappingContext neo4jMappingContext) {
		this.neo4jMappingContext = neo4jMappingContext;
	}

	@Override
	public int getOrder() {
		return AuditingBeforeBindCallback.NEO4J_AUDITING_ORDER + 15;
	}

	@Override
	public Object onAfterConvert(Object instance, Neo4jPersistentEntity<Object> entity, MapAccessor source) {

		return this.neo4jMappingContext.invokePostLoad(entity, instance);
	}

}
