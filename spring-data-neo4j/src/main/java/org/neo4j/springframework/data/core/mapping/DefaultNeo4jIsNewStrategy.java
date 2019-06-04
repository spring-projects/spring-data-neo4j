/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.mapping;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

import org.neo4j.springframework.data.core.schema.Id;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of a {@link IsNewStrategy} that follows our supported identifiers and generators.
 * Entities will be treated as new:
 * <ul>
 * <li>when using internally generated (database) ids and the id property is {@literal null} or of a numeric primitive less than or equal {@literal 0},</li>
 * <li>when using externally generated values and the id is {@literal null},</li>
 * <li>when using assigned values without a version property or with a version property that is {@literal null}.</li>
 * </ul>
 * <p/>
 * An entity will not be treated as new
 * <ul>
 * <li>when using internally generated (database) ids and the id property has a non-null value greater than {@literal 0},</li>
 * <li>when using externally generated values and the id property is not {@literal null},</li>
 * <li>when using assigned values together with {@link org.springframework.data.annotation.Version @Version} which has already a value not equal to {@literal null} or {@literal 0}.</li>
 * </ul>
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class DefaultNeo4jIsNewStrategy implements IsNewStrategy {

	static IsNewStrategy basedOn(Neo4jPersistentEntity<?> entityMetaData) {

		Assert.notNull(entityMetaData, "Entity meta data must not be null.");

		Id.Strategy idStrategy = entityMetaData.getIdDescription().getIdStrategy();
		Class<?> valueType = entityMetaData.getRequiredIdProperty().getType();

		if (idStrategy == Id.Strategy.EXTERNALLY_GENERATED && valueType.isPrimitive()) {
			throw new IllegalArgumentException(String.format("Cannot use %s with externally generated, primitive ids.",
				DefaultNeo4jIsNewStrategy.class.getName()));
		}

		Function<Object, Object> valueLookup;
		Neo4jPersistentProperty versionProperty = entityMetaData.getVersionProperty();
		if (idStrategy == Id.Strategy.ASSIGNED) {
			if (versionProperty == null) {
				log.warn("Instances of " + entityMetaData.getType()
					+ " with an assigned id will always be treated as new without version property!");
				valueType = Void.class;
				valueLookup = source -> null;
			} else {
				valueType = versionProperty.getType();
				valueLookup = source -> entityMetaData.getPropertyAccessor(source).getProperty(versionProperty);
			}
		} else {
			valueLookup = source -> entityMetaData.getIdentifierAccessor(source).getIdentifier();
		}

		return new DefaultNeo4jIsNewStrategy(idStrategy, valueType, valueLookup);
	}

	private final Id.Strategy strategy;

	private final Class<?> valueType;

	private @Nullable final Function<Object, Object> valueLookup;

	/*
	 * (non-Javadoc)
	 * @see IsNewStrategy#isNew(Object)
	 */
	@Override
	public boolean isNew(Object entity) {

		Object value = valueLookup.apply(entity);
		if (strategy.isInternal()) {

			boolean isNew = false;
			if (value != null && valueType.isPrimitive() && Number.class.isInstance(value)) {
				isNew = ((Number) value).longValue() <= 0;
			} else {
				isNew = value == null;
			}

			return isNew;
		} else if (strategy == Id.Strategy.EXTERNALLY_GENERATED) {
			return value == null;
		} else if (strategy == Id.Strategy.ASSIGNED) {
			if (valueType != null && !valueType.isPrimitive()) {
				return value == null;
			}

			if (Number.class.isInstance(value)) {
				return ((Number) value).longValue() == 0;
			}
		}

		throw new IllegalArgumentException(
			String
				.format("Could not determine whether %s is new! Unsupported identifier or version property!", entity));

	}
}
