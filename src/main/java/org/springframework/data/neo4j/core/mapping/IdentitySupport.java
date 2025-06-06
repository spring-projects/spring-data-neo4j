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

import java.util.function.Function;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.TypeSystem;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * This class is <strong>not</strong> part of any public API and will be changed without
 * further notice as needed. It's primary goal is to mitigate the changes in Neo4j5, which
 * introduces the notion of an {@literal element id} for both nodes and relationships
 * while deprecating {@literal id} at the same time. The identity support allows to
 * isolate our calls deprecated API in one central place and will exist for SDN 7 only to
 * make SDN 7 work with both Neo4j 4.4 and Neo4j 5.x.
 *
 * @author Michael J. Simons
 * @since 7.0
 */
@API(status = INTERNAL)
public final class IdentitySupport {

	private IdentitySupport() {
	}

	/**
	 * Retrieves the element id of an entity.
	 * @param entity the entity container as received from the server.
	 * @return the internal id
	 */
	public static String getElementId(Entity entity) {
		return entity.elementId();
	}

	/**
	 * Retrieves an identity either from attributes inside the row or if it is an actual
	 * entity, with the dedicated accessors.
	 * @param row a query result row
	 * @return an internal id
	 */
	@Nullable public static String getElementId(MapAccessor row) {
		if (row instanceof Entity entity) {
			return getElementId(entity);
		}

		var columnToUse = Constants.NAME_OF_ELEMENT_ID;
		Value value = row.get(columnToUse);
		if (value == null || value.isNull()) {
			return null;
		}
		if (value.hasType(TypeSystem.getDefault().NUMBER())) {
			return value.asNumber().toString();
		}
		return value.asString();
	}

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	@Nullable public static Long getInternalId(MapAccessor row) {
		if (row instanceof Entity entity) {
			return entity.id();
		}

		var columnToUse = Constants.NAME_OF_INTERNAL_ID;
		if (row.get(columnToUse) == null || row.get(columnToUse).isNull()) {
			return null;
		}

		return row.get(columnToUse).asLong();
	}

	@Nullable public static String getPrefixedElementId(MapAccessor queryResult, @Nullable String seed) {
		if (queryResult instanceof Node) {
			return "N" + getElementId(queryResult);
		}
		else if (queryResult instanceof Relationship) {
			return "R" + seed + getElementId(queryResult);
		}
		else if (!(queryResult.get(Constants.NAME_OF_ELEMENT_ID) == null
				|| queryResult.get(Constants.NAME_OF_ELEMENT_ID).isNull())) {
			Value value = queryResult.get(Constants.NAME_OF_ELEMENT_ID);
			if (value.hasType(TypeSystem.getDefault().NUMBER())) {
				return "N" + value.asNumber();
			}
			return "N" + value.asString();
		}

		return null;
	}

	public static Function<MapAccessor, Object> mapperForRelatedIdValues(@Nullable Neo4jPersistentProperty idProperty) {
		boolean deprecatedHolder = idProperty != null
				&& Neo4jPersistentEntity.DEPRECATED_GENERATED_ID_TYPES.contains(idProperty.getType());
		return deprecatedHolder ? IdentitySupport::getInternalId : IdentitySupport::getElementId;
	}

}
