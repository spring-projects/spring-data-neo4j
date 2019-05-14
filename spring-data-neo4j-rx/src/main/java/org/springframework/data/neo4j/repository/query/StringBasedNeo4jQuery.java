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
package org.springframework.data.neo4j.repository.query;

import java.util.Collections;
import java.util.Optional;

import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Implementation of {@link RepositoryQuery} for String based custom Cypher query.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
final class StringBasedNeo4jQuery extends AbstractNeo4jQuery {

	private final String cypherQuery;

	private final boolean countQuery;
	private final boolean existsQuery;
	private final boolean deleteQuery;

	StringBasedNeo4jQuery(NodeManager nodeManager, Neo4jMappingContext mappingContext, Neo4jQueryMethod queryMethod,
		String cypherQuery,
		Optional<Query> optionalQueryAnnotation) {

		super(nodeManager, mappingContext, queryMethod);

		this.cypherQuery = cypherQuery;

		if (optionalQueryAnnotation.isPresent()) {
			Query queryAnnotation = optionalQueryAnnotation.get();
			countQuery = queryAnnotation.count();
			existsQuery = queryAnnotation.exists();
			deleteQuery = queryAnnotation.delete();
		} else {
			countQuery = false;
			existsQuery = false;
			deleteQuery = false;
		}
	}

	@Override
	protected PreparedQuery<?> prepareQuery(Object[] parameters) {

		return PreparedQuery.queryFor(super.domainType)
			.withCypherQuery(cypherQuery)
			.withParameters(Collections.emptyMap()) // TODO Map parameters.
			.usingMappingFunction(mappingContext.getMappingFunctionFor(super.domainType).orElse(null)) // Null is fine
			.build();
	}

	@Override
	public boolean isCountQuery() {
		return countQuery;
	}

	@Override
	public boolean isExistsQuery() {
		return existsQuery;
	}

	@Override
	public boolean isDeleteQuery() {
		return deleteQuery;
	}

	@Override
	protected boolean isLimiting() {
		return false;
	}
}
