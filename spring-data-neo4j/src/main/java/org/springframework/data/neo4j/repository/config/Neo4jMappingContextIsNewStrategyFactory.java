/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.repository.config;

import java.util.Collections;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.MappingContextIsNewStrategyFactory;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.lang.Nullable;

/**
 * Custom MappingContextIsNewStrategyFactory with overriden doGetIsNewStrategy
 *
 * @author Frantisek Hartman
 */
public class Neo4jMappingContextIsNewStrategyFactory extends MappingContextIsNewStrategyFactory {

	private final PersistentEntities context;

	public Neo4jMappingContextIsNewStrategyFactory(MappingContext<? extends PersistentEntity<?, ?>, ?> context) {
		this(new PersistentEntities(Collections.singletonList(context)));
	}

	public Neo4jMappingContextIsNewStrategyFactory(PersistentEntities entities) {
		super(entities);
		this.context = entities;
	}

	@Nullable
	@Override
	protected IsNewStrategy doGetIsNewStrategy(Class<?> type) {
		return new IsNewStrategy() {
			@Override
			public boolean isNew(Object o) {
				PersistentEntity<?, ? extends PersistentProperty<?>> entity = context.getRequiredPersistentEntity(type);

				PersistentProperty<? extends PersistentProperty<?>> property = entity.getRequiredIdProperty();
				Object value = property.getOwner().getPropertyAccessor(o).getProperty(property);

				return value == null || (value instanceof Long && ((Long) value) < 0);
			}
		};
	}
}
