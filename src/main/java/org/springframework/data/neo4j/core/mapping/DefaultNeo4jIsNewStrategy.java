/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogAccessor;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.util.Assert;

/**
 * Implementation of a {@link IsNewStrategy} that follows our supported identifiers and
 * generators. Entities will be treated as new:
 * <ul>
 * <li>when using internally generated (database) ids and the id property is
 * {@literal null} or of a numeric primitive less than or equal {@literal 0},</li>
 * <li>when using externally generated values and the id is {@literal null},</li>
 * <li>when using assigned values without a version property or with a version property
 * that is {@literal null}.</li>
 * </ul>
 * <p>
 * An entity will not be treated as new
 * <ul>
 * <li>when using internally generated (database) ids and the id property has a non-null
 * value greater than {@literal 0},</li>
 * <li>when using externally generated values and the id property is not
 * {@literal null},</li>
 * <li>when using assigned values together with
 * {@link org.springframework.data.annotation.Version @Version} which has already a value
 * not equal to {@literal null} or {@literal 0}.</li>
 * </ul>
 *
 * @author Michael J. Simons
 * @since 5.1.20
 */
final class DefaultNeo4jIsNewStrategy implements IsNewStrategy {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(DefaultNeo4jIsNewStrategy.class));

	private final IdDescription idDescription;

	private final Class<?> valueType;

	private final Function<Object, Object> valueLookup;

	private DefaultNeo4jIsNewStrategy(IdDescription idDescription, Class<?> valueType,
			Function<Object, Object> valueLookup) {
		this.idDescription = idDescription;
		this.valueType = valueType;
		this.valueLookup = valueLookup;
	}

	static IsNewStrategy basedOn(Neo4jPersistentEntity<?> entityMetaData) {

		Assert.notNull(entityMetaData, "Entity meta data must not be null");

		IdDescription idDescription = Objects.requireNonNull(entityMetaData.getIdDescription(),
				() -> "Cannot determine id description for entity %s".formatted(entityMetaData.getType()));
		Class<?> valueType = entityMetaData.getRequiredIdProperty().getType();

		if (idDescription.isExternallyGeneratedId() && valueType.isPrimitive()) {
			throw new IllegalArgumentException(String.format("Cannot use %s with externally generated, primitive ids",
					DefaultNeo4jIsNewStrategy.class.getName()));
		}

		Function<Object, Object> valueLookup;
		Neo4jPersistentProperty versionProperty = entityMetaData.getVersionProperty();
		if (idDescription.isAssignedId()) {
			if (versionProperty == null) {
				log.warn(() -> "Instances of " + entityMetaData.getType()
						+ " with an assigned id will always be treated as new without version property");
				valueType = Void.class;
				valueLookup = source -> null;
			}
			else {
				valueType = versionProperty.getType();
				valueLookup = source -> entityMetaData.getPropertyAccessor(source).getProperty(versionProperty);
			}
		}
		else {
			valueLookup = source -> entityMetaData.getIdentifierAccessor(source).getIdentifier();
		}

		return new DefaultNeo4jIsNewStrategy(idDescription, valueType, valueLookup);
	}

	@Override
	public boolean isNew(Object entity) {

		Object value = this.valueLookup.apply(entity);
		if (this.idDescription.isInternallyGeneratedId()) {

			boolean isNew;
			if (value != null && this.valueType.isPrimitive() && value instanceof Number) {
				isNew = ((Number) value).longValue() < 0;
			}
			else {
				isNew = value == null;
			}

			return isNew;
		}
		else if (this.idDescription.isExternallyGeneratedId()) {
			return value == null;
		}
		else if (this.idDescription.isAssignedId()) {
			if (this.valueType != null && !this.valueType.isPrimitive()) {
				return value == null;
			}

			if (value instanceof Number) {
				return ((Number) value).longValue() == 0;
			}
		}

		throw new IllegalArgumentException(String
			.format("Could not determine whether %s is new! Unsupported identifier or version property", entity));

	}

}
