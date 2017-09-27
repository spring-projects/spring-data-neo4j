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

	public Neo4jIsNewAwareAuditingHandler(MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {
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
