/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.util.TypeInformation;

/**
 * This class implements Spring Data's PersistentEntity interface, scavenging the required data from the OGM's mapping
 * classes in order to for SDN to play nicely with Spring Data REST. The main thing to note is that this class is
 * effectively a shim for ClassInfo. We don't reload all the mapping information again.
 * <p>
 * These attributes do not appear to be used/needed for SDN 4 to inter-operate correctly with SD-REST:
 * </p>
 * <ul>
 * <li>typeAlias</li>
 * <li>typeInformation</li>
 * <li>preferredConstructor (we always use the default constructor)</li>
 * <li>versionProperty</li>
 * </ul>
 * Consequently their associated getter methods always return default values of null or [true|false] However, because
 * these method calls are not expected, we also log a warning message if they get invoked
 *
 * @author Vince Bickers
 * @author Adam George
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 4.0.0
 */
public class Neo4jPersistentEntity<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty> {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jPersistentEntity.class);

	/**
	 * Constructs a new {@link Neo4jPersistentEntity} based on the given type information.
	 *
	 * @param information The {@link TypeInformation} upon which to base this persistent entity.
	 */
	public Neo4jPersistentEntity(TypeInformation<T> information) {
		super(information);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#getFallbackIsNewStrategy()
	 */
	@Override
	protected IsNewStrategy getFallbackIsNewStrategy() {
		return new Neo4jIsNewStrategy(this);
	}

	/**
	 * Custom {@link IsNewStrategy} to also consider entities with identifiers of negative Long values new.
	 *
	 * @author Frantisek Hartman
	 * @author Oliver Gierke
	 * @see DATAGRAPH-1031
	 */
	private static class Neo4jIsNewStrategy implements IsNewStrategy {

		private final Neo4jPersistentEntity<?> entity;

		public Neo4jIsNewStrategy(Neo4jPersistentEntity<?> entity) {
			this.entity = entity;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.support.IsNewStrategy#isNew(java.lang.Object)
		 */
		@Override
		public boolean isNew(Object bean) {

			PersistentProperty<? extends PersistentProperty<?>> property = entity.getRequiredIdProperty();
			Object value = entity.getPropertyAccessor(bean).getProperty(property);

			return value == null || (value instanceof Long && ((Long) value) < 0);
		}
	}
}
