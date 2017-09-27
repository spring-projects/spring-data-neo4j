/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
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
