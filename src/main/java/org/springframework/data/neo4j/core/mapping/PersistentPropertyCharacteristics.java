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

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * The characteristics of a {@link Neo4jPersistentProperty} can diverge from what is by
 * default derived from the annotated classes. Diverging characteristics are requested by
 * the {@link Neo4jMappingContext} prior to creating a persistent property. Additional
 * providers of characteristics may be registered with the mapping context.
 *
 * @author Michael J. Simons
 * @since 6.3.7
 */
@API(status = STABLE, since = "6.3.7")
public interface PersistentPropertyCharacteristics {

	/**
	 * Default characteristics.
	 * @return characteristics applying the defaults
	 */
	static PersistentPropertyCharacteristics useDefaults() {
		return new PersistentPropertyCharacteristics() {
		};
	}

	/**
	 * Treats the property to which the instance is applied as transient.
	 * @return characteristics to treat a property as transient
	 */
	static PersistentPropertyCharacteristics treatAsTransient() {
		return new PersistentPropertyCharacteristics() {
			@Override
			public Boolean isTransient() {
				return true;
			}
		};
	}

	/**
	 * Treats the property to which the instance is applied as read only.
	 * @return characteristics to treat a property as read only
	 */
	static PersistentPropertyCharacteristics treatAsReadOnly() {
		return new PersistentPropertyCharacteristics() {
			@Override
			public Boolean isReadOnly() {
				return true;
			}
		};
	}

	/**
	 * Return {@literal true} to mark a property as transient.
	 * @return {@literal null} to leave the defaults, {@literal true} or {@literal false}
	 * otherwise
	 */
	@Nullable default Boolean isTransient() {
		return null;
	}

	/**
	 * Return {@literal true} to mark a property as read only.
	 * @return {@literal null} to leave the defaults, {@literal true} or {@literal false}
	 * otherwise
	 */
	@Nullable default Boolean isReadOnly() {
		return null;
	}

}
