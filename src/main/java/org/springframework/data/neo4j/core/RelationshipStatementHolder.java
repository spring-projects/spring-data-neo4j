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
package org.springframework.data.neo4j.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.cypherdsl.core.Statement;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.schema.CypherGenerator;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;

/**
 * The {@link RelationshipStatementHolder} holds the Cypher Statement to create a relationship as well as the optional
 * properties that describe the relationship in case of more then a simple relationship. By holding the relationship
 * creation cypher together with the properties, we can reuse the same logic in the {@link Neo4jTemplate} as well as in
 * the {@link ReactiveNeo4jTemplate}.
 *
 * @author Philipp Tölle
 * @author Michael J. Simons
 * @since 6.0
 */
final class RelationshipStatementHolder {
	private final Statement relationshipCreationQuery;
	private final Map<String, Object> properties;

	private RelationshipStatementHolder(@NonNull Statement relationshipCreationQuery) {
		this(relationshipCreationQuery, Collections.emptyMap());
	}

	private RelationshipStatementHolder(@NonNull Statement relationshipCreationQuery,
			@NonNull Map<String, Object> properties) {
		this.relationshipCreationQuery = relationshipCreationQuery;
		this.properties = properties;
	}

	Statement getRelationshipCreationQuery() {
		return relationshipCreationQuery;
	}

	Map<String, Object> getProperties() {
		return properties;
	}

	static RelationshipStatementHolder createStatement(Neo4jMappingContext neo4jMappingContext,
			Neo4jPersistentEntity<?> neo4jPersistentEntity, NestedRelationshipContext relationshipContext,
			Long relatedInternalId, Object relatedValue) {

		if (relationshipContext.hasRelationshipWithProperties()) {
			return createStatementForRelationShipWithProperties(neo4jMappingContext, neo4jPersistentEntity,
					relationshipContext, relatedInternalId, (Map.Entry) relatedValue);
		} else {
			return createStatementForRelationshipWithoutProperties(neo4jMappingContext, neo4jPersistentEntity,
					relationshipContext, relatedInternalId, relatedValue);
		}
	}

	private static RelationshipStatementHolder createStatementForRelationShipWithProperties(
			Neo4jMappingContext neo4jMappingContext, Neo4jPersistentEntity<?> neo4jPersistentEntity,
			NestedRelationshipContext relationshipContext, Long relatedInternalId, Map.Entry relatedValue) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.createRelationshipWithPropertiesCreationQuery(
				neo4jPersistentEntity, relationshipContext.getRelationship(), relatedInternalId);
		Map<String, Object> propMap = new HashMap<>();
		neo4jMappingContext.getConverter().write(relatedValue.getValue(), propMap);

		return new RelationshipStatementHolder(relationshipCreationQuery, propMap);
	}

	private static RelationshipStatementHolder createStatementForRelationshipWithoutProperties(
			Neo4jMappingContext neo4jMappingContext, Neo4jPersistentEntity<?> neo4jPersistentEntity,
			NestedRelationshipContext relationshipContext, Long relatedInternalId, Object relatedValue) {

		String relationshipType;
		if (!relationshipContext.getRelationship().isDynamic()) {
			relationshipType = null;
		} else {
			TypeInformation<?> keyType = relationshipContext.getInverse().getTypeInformation().getRequiredComponentType();
			Object key = ((Map.Entry<?, ?>) relatedValue).getKey();
			relationshipType = neo4jMappingContext.getConverter().writeValueFromProperty(key, keyType).asString();
		}

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.createRelationshipCreationQuery(
				neo4jPersistentEntity, relationshipContext.getRelationship(), relationshipType, relatedInternalId);
		return new RelationshipStatementHolder(relationshipCreationQuery);
	}
}
