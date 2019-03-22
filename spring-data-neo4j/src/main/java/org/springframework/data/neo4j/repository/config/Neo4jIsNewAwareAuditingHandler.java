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

package org.springframework.data.neo4j.repository.config;

import java.util.Collections;

import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.support.IsNewStrategyFactory;
import org.springframework.util.Assert;

/**
 * IsNewAwareAuditingHandler which overrides markAudited to provide our own Neo4jMappingContextIsNewStrategyFactory
 *
 * @author Frantisek Hartman
 */
public class Neo4jIsNewAwareAuditingHandler extends IsNewAwareAuditingHandler {

	private IsNewStrategyFactory isNewStrategyFactory;

	public Neo4jIsNewAwareAuditingHandler(
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {
		this(new PersistentEntities(Collections.singletonList(mappingContext)));
	}

	public Neo4jIsNewAwareAuditingHandler(PersistentEntities entities) {
		super(entities);

		isNewStrategyFactory = new Neo4jMappingContextIsNewStrategyFactory(entities);
	}

	public void markAudited(Object object) {
		Assert.notNull(object, "Source object must not be null!");
		if (this.isAuditable(object)) {
			IsNewStrategy strategy = this.isNewStrategyFactory.getIsNewStrategy(object.getClass());
			if (strategy.isNew(object)) {
				this.markCreated(object);
			} else {
				this.markModified(object);
			}

		}
	}
}
