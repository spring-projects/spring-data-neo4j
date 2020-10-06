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
package org.springframework.data.neo4j.repository.support;

import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Neo4j specific contract for {@link EntityInformation entity informations}.
 *
 * @author Michael J. Simons
 * @param <T> The type of the entity
 * @param <ID> The type of the id
 * @soundtrack Bear McCreary - Battlestar Galactica Season 1
 * @since 6.0
 */
public interface Neo4jEntityInformation<T, ID> extends EntityInformation<T, ID> {

	/**
	 * @return The full schema based description for the underlying entity.
	 */
	Neo4jPersistentEntity<T> getEntityMetaData();
}
