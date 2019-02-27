/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.query;

import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Represents a String based custom Cypher query.
 *
 * @author Gerrit Meier
 */
public class StringBasedNeo4jQuery extends AbstractNeo4jQuery {

	private final Neo4jQueryMethod queryMethod;
	private final Neo4jOperations neo4jOperations;

	public StringBasedNeo4jQuery(Neo4jQueryMethod queryMethod, Neo4jOperations neo4jOperations) {

		this.queryMethod = queryMethod;
		this.neo4jOperations = neo4jOperations;
	}

	@Override
	public Object execute(Object[] parameters) {
		return neo4jOperations.executeQuery(queryMethod.getAnnotatedQuery());
	}

	@Override
	public QueryMethod getQueryMethod() {
		return null;
	}
}
