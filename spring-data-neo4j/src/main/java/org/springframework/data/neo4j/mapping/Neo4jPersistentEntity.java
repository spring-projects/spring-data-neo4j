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

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty.PropertyType;
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
 * @author Michael J. Simons
 * @since 4.0.0
 */
public class Neo4jPersistentEntity<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty> {

	/**
	 * Constructs a new {@link Neo4jPersistentEntity} based on the given type information.
	 *
	 * @param information The {@link TypeInformation} upon which to base this persistent entity.
	 */
	Neo4jPersistentEntity(TypeInformation<T> information) {
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#returnPropertyIfBetterIdPropertyCandidateOrNull(PersistentProperty)
	 */
	@Override
	protected Neo4jPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(Neo4jPersistentProperty property) {

		if (!property.isIdProperty()) {
			return null;
		}

		Neo4jPersistentProperty existingIdProperty = this.getIdProperty();
		Neo4jPersistentProperty preferredIdProperty = existingIdProperty;

		if (existingIdProperty == null) {
			preferredIdProperty = property;
		} else if (existingIdProperty.getPropertyType() == property.getPropertyType()) {
			throw new MappingException(
					String.format("Attempt to add id property %s but already have property %s registered "
									+ "as id. Check your mapping configuration!", property.getField(),
							existingIdProperty.getField()));
		} else if (existingIdProperty.getPropertyType() == PropertyType.INTERNAL_ID_PROPERTY && property.getPropertyType() == PropertyType.ID_PROPERTY) {
			preferredIdProperty = property;
		}
		return preferredIdProperty;
	}

	/**
	 * Custom {@link IsNewStrategy} to also consider entities with identifiers of negative Long values new.
	 * See also DATAGRAPH-1031.
	 *
	 * @author Frantisek Hartman
	 * @author Oliver Gierke
	 */
	private static class Neo4jIsNewStrategy implements IsNewStrategy {

		private final Neo4jPersistentEntity<?> entity;

		private Neo4jIsNewStrategy(Neo4jPersistentEntity<?> entity) {
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
