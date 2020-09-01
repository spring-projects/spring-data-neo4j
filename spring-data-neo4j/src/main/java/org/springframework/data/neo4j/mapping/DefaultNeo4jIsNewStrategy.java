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
package org.springframework.data.neo4j.mapping;

import java.util.function.Function;

import org.neo4j.ogm.id.InternalIdStrategy;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of a {@link IsNewStrategy} that follows our supported identifiers and generators. Entities will be
 * treated as new:
 * <ul>
 * <li>when using internally generated (database) ids and the id property is {@literal null} or of a numeric primitive
 * less than or equal {@literal 0},</li>
 * <li>when using externally generated values and the id is {@literal null},</li>
 * <li>when using assigned values without a version property or with a version property that is {@literal null}.</li>
 * </ul>
 * <p>
 * An entity will not be treated as new
 * <ul>
 * <li>when using internally generated (database) ids and the id property has a non-null value greater than
 * {@literal 0},</li>
 * <li>when using externally generated values and the id property is not {@literal null},</li>
 * <li>when using assigned values together with {@link org.springframework.data.annotation.Version @Version} which has
 * already a value not equal to {@literal null} or {@literal 0}.</li>
 * </ul>
 *
 * @author Michael J. Simons
 * @since 5.1.20
 */
class DefaultNeo4jIsNewStrategy implements IsNewStrategy {

	private static final Logger log = LoggerFactory.getLogger(DefaultNeo4jIsNewStrategy.class);

	static IsNewStrategy basedOn(Neo4jPersistentEntity<?> entity, MetaData ogmMetadata) {

		Assert.notNull(entity, "Entity meta data must not be null.");

		ClassInfo classInfo = ogmMetadata.classInfo(entity.getType());
		boolean internallyGeneratedId = classInfo.hasIdentityField();
		boolean externallyGeneratedId =
				classInfo.idStrategyClass() != null && InternalIdStrategy.class != classInfo.idStrategyClass();
		boolean assignedId = !internallyGeneratedId && classInfo.idStrategyClass() == null;

		Class<?> valueType;
		if (classInfo.hasIdentityField()) {
			valueType = classInfo.identityField().type();
		} else if (classInfo.hasPrimaryIndexField()) {
			valueType = classInfo.primaryIndexField().type();
		} else {
			throw new IllegalStateException(
					String.format("Required identifier property not found for %s!", entity.getType()));
		}

		if (externallyGeneratedId && valueType.isPrimitive()) {
			throw new IllegalArgumentException(String.format("Cannot use %s with externally generated, primitive ids.",
					DefaultNeo4jIsNewStrategy.class.getName()));
		}

		Function<Object, Object> valueLookup;
		Neo4jPersistentProperty versionProperty = entity.getVersionProperty();
		if (assignedId) {
			if (versionProperty == null) {
				log.warn("Instances of " + entity.getType()
						+ " with an assigned id will always be treated as new without version property!");
				valueType = Void.class;
				valueLookup = source -> null;
			} else {
				valueType = versionProperty.getType();
				valueLookup = source -> entity.getPropertyAccessor(source).getProperty(versionProperty);
			}
		} else {
			valueLookup = source -> entity.getIdentifierAccessor(source).getIdentifier();
		}

		return new DefaultNeo4jIsNewStrategy(internallyGeneratedId, externallyGeneratedId, assignedId, valueType,
				valueLookup);
	}

	private final boolean internallyGeneratedId;

	private final boolean externallyGeneratedId;

	private final boolean assignedId;

	private final Class<?> valueType;

	private @Nullable final Function<Object, Object> valueLookup;

	private DefaultNeo4jIsNewStrategy(boolean internallyGeneratedId, boolean externallyGeneratedId, boolean assignedId,
			Class<?> valueType, @Nullable Function<Object, Object> valueLookup) {
		this.internallyGeneratedId = internallyGeneratedId;
		this.externallyGeneratedId = externallyGeneratedId;
		this.assignedId = assignedId;
		this.valueType = valueType;
		this.valueLookup = valueLookup;
	}

	/*
	 * (non-Javadoc)
	 * @see IsNewStrategy#isNew(Object)
	 */
	@Override
	public boolean isNew(Object entity) {

		Object value = valueLookup.apply(entity);
		if (internallyGeneratedId) {
			return value == null || (value instanceof Long && ((Long) value) < 0);
		} else if (externallyGeneratedId) {
			return value == null;
		} else if (assignedId) {
			if (valueType != null && !valueType.isPrimitive()) {
				return value == null;
			}

			if (Number.class.isInstance(value)) {
				return ((Number) value).longValue() == 0;
			}
		}

		throw new IllegalArgumentException(
				String.format("Could not determine whether %s is new! Unsupported identifier or version property!",
						entity));

	}
}
