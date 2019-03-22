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
package org.springframework.data.neo4j.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
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

	@Override
	public boolean hasVersionProperty() {
		logger.debug("[entity].hasVersionProperty() returns false"); // by design
		return false;
	}

	@Override
	public Neo4jPersistentProperty getVersionProperty() {
		logger.debug("[entity].getVersionProperty() returns null"); // by design
		return null;
	}

	@Override
	public boolean isVersionProperty(PersistentProperty<?> property) {
		logger.debug("[entity].isIdProperty({}) returns false", property); // again, by design
		return false;
	}

}
