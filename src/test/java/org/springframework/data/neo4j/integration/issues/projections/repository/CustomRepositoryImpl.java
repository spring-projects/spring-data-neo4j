/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.projections.repository;

import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.integration.issues.projections.model.SourceNodeA;
import org.springframework.data.neo4j.integration.issues.projections.projection.SourceNodeAProjection;

/**
 * @author Michael J. Simons
 */
class CustomRepositoryImpl implements CustomRepository {

	private final Neo4jOperations neo4jOperations;

	CustomRepositoryImpl(Neo4jOperations neo4jOperations) {
		this.neo4jOperations = neo4jOperations;
	}

	@Override
	public SourceNodeAProjection saveWithProjection(SourceNodeA sourceNodeA) {
		return this.neo4jOperations.saveAs(sourceNodeA, SourceNodeAProjection.class);
	}

}
