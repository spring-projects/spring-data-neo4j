/*
 * Copyright 2011-2022 the original author or authors.
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

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * This class is <strong>not</strong> part of any public API and will be changed without further notice as needed. It's
 * primary goal is to mitigate the changes in Neo4j5, which introduces the notion of an {@literal element id} for both nodes
 * and relationships while deprecating {@literal id} at the same time. The identity support allows to isolate our calls
 * deprecated API in one central place and will exists for SDN 7 only to make SDN 7 work with both Neo4j 4.4 and Neo4j 5.x.
 *
 * @author Michael J. Simons
 * @soundtrack Buckethead - SIGIL Soundtrack
 * @since 7.0
 */
@API(status = INTERNAL)
public final class IdentitySupport {

	private IdentitySupport() {
	}

	/**
	 * Updates the internal id of a given client side entity from a server side entity using a property accessor.
	 * Does nothing if the local entity does not use internally generated ids.
	 *
	 * @param entityMetaData   The entity's meta data
	 * @param propertyAccessor An accessor to the entity
	 * @param entity           As received via the driver
	 */
	public static void updateInternalId(Neo4jPersistentEntity<?> entityMetaData,
			PersistentPropertyAccessor<?> propertyAccessor, Entity entity) {

		if (!entityMetaData.isUsingInternalIds()) {
			return;
		}

		propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), getInternalId(entity));
	}

	/**
	 * @param entity The entity container as received from the server.
	 * @return The internal id
	 */
	@SuppressWarnings("deprecation")
	public static Long getInternalId(Entity entity) {
		return entity.id();
	}

	/**
	 * Retrieves an identity either from attributes inside the row or if it is an actual entity, with the dedicated accessors.
	 *
	 * @param row A query result row
	 * @return An internal id
	 */
	@Nullable
	public static Long getInternalId(@NonNull MapAccessor row) {
		if (row instanceof Entity entity) {
			return getInternalId(entity);
		}

		var columnToUse = Constants.NAME_OF_INTERNAL_ID;
		if (row.get(columnToUse) == null || row.get(columnToUse).isNull()) {
			return null;
		}

		return row.get(columnToUse).asLong();
	}

	/**
	 * Returns the start id of the relationship
	 *
	 * @param relationship A relationship to retrieve the start identity
	 * @return An internal id
	 */
	@SuppressWarnings("deprecation")
	public static Long getStartId(Relationship relationship) {
		return relationship.startNodeId();
	}

	/**
	 * Returns the end id of the relationship
	 *
	 * @param relationship A relationship to retrieve the end identity
	 * @return An internal id
	 */
	@SuppressWarnings("deprecation")
	public static Long getEndId(Relationship relationship) {
		return relationship.endNodeId();
	}

	public static Long getInternalId(TypeSystem typeSystem, Record row) {
		return getInternalId(row);
	}
}
