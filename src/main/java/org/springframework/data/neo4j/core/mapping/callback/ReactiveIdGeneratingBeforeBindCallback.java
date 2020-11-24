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
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;

/**
 * Callback used to call the ID generator configured for an entity just before binding.
 *
 * @author Michael J. Simons
 * @soundtrack Various - Kung Fury (Original Motion Picture Soundtrack)
 * @since 6.0
 */
final class ReactiveIdGeneratingBeforeBindCallback implements ReactiveBeforeBindCallback<Object>, Ordered {

	private final IdPopulator idPopulator;

	ReactiveIdGeneratingBeforeBindCallback(Neo4jMappingContext neo4jMappingContext) {
		this.idPopulator = new IdPopulator(neo4jMappingContext);
	}

	@Override
	public Publisher<Object> onBeforeBind(Object entity) {

		return Mono.fromSupplier(() -> idPopulator.populateIfNecessary(entity));
	}

	@Override
	public int getOrder() {
		return ReactiveAuditingBeforeBindCallback.NEO4J_REACTIVE_AUDITING_ORDER + 10;
	}
}
