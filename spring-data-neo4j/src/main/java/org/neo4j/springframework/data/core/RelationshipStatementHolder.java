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
package org.neo4j.springframework.data.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.schema.CypherGenerator;

/**
 * The {@link RelationshipStatementHolder} holds the Cypher Statement to create a relationship as well as the optional
 * properties that describe the relationship in case of more then a simple relationship.
 * By holding the relationship creation cypher together with the properties, we can reuse the same logic in the
 * {@link Neo4jTemplate} as well as in the {@link ReactiveNeo4jTemplate}.
 *
 * @author Philipp Tölle
 * @since 1.0
 */
final class RelationshipStatementHolder {
	private final Statement relationshipCreationQuery;
	private final Map<String, Object> properties;

	private RelationshipStatementHolder(@NotNull Statement relationshipCreationQuery) {
		this.relationshipCreationQuery = relationshipCreationQuery;
		this.properties = Collections.emptyMap();
	}

	private RelationshipStatementHolder(
		@NotNull Statement relationshipCreationQuery,
		@NotNull Map<String, Object> properties) {
		this.relationshipCreationQuery = relationshipCreationQuery;
		this.properties = properties;
	}

	Statement getRelationshipCreationQuery() {
		return relationshipCreationQuery;
	}

	Map<String, Object> getProperties() {
		return properties;
	}

	static RelationshipStatementHolder createStatementForRelationShipWithProperties(
		Neo4jMappingContext neo4jMappingContext,
		Neo4jPersistentEntity<?> neo4jPersistentEntity,
		NestedRelationshipContext relationshipContext,
		Long relatedInternalId,
		Map.Entry relatedValue) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE
			.createRelationshipWithPropertiesCreationQuery(
				neo4jPersistentEntity,
				relationshipContext.getRelationship(),
				relatedInternalId
			);
		Map<String, Object> propMap = new HashMap<>();
		neo4jMappingContext.getConverter().write(relatedValue.getValue(), propMap);

		return new RelationshipStatementHolder(relationshipCreationQuery, propMap);
	}

	static RelationshipStatementHolder createStatementForRelationshipWithoutProperties(
		Neo4jPersistentEntity<?> neo4jPersistentEntity,
		NestedRelationshipContext relationshipContext,
		Long relatedInternalId,
		Object relatedValue) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE
			.createRelationshipCreationQuery(neo4jPersistentEntity,
				relationshipContext.getRelationship(),
				relatedValue instanceof Map.Entry ? ((Map.Entry<String, ?>) relatedValue).getKey() : null,
				relatedInternalId);
		return new RelationshipStatementHolder(relationshipCreationQuery);
	}
}

