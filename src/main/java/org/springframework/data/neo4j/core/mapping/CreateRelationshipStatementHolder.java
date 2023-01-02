/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.lang.NonNull;

/**
 * The {@link CreateRelationshipStatementHolder} holds the Cypher Statement to create a relationship as well as the optional
 * properties that describe the relationship in case of more than a simple relationship. By holding the relationship
 * creation cypher together with the properties, we can reuse the same logic in the {@link Neo4jTemplate} as well as in
 * the {@link ReactiveNeo4jTemplate}.
 *
 * @author Philipp Tölle
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class CreateRelationshipStatementHolder {

	private final Statement statement;
	private final Map<String, Object> properties;

	CreateRelationshipStatementHolder(@NonNull Statement statement, @NonNull Map<String, Object> properties) {
		this.statement = statement;
		this.properties = properties;
	}

	public Statement getStatement() {
		return statement;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public CreateRelationshipStatementHolder addProperty(String key, Object property) {
		Map<String, Object> newProperties = new HashMap<>(this.properties);
		newProperties.put(key, property);
		return new CreateRelationshipStatementHolder(this.statement, newProperties);
	}
}
