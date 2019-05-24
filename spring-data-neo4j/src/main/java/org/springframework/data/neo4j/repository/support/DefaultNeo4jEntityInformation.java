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
package org.springframework.data.neo4j.repository.support;

import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.neo4j.core.cypher.Expression;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.repository.query.CypherAdapterUtils;
import org.springframework.data.repository.core.support.PersistentEntityInformation;

/**
 * Default implementation of Neo4j specific entity information.
 *
 * @author Michael J. Simons
 * @soundtrack Bear McCreary - Battlestar Galactica Season 1
 * @since 1.0
 */
final class DefaultNeo4jEntityInformation<T, ID> extends PersistentEntityInformation<T, ID>
	implements Neo4jEntityInformation<T, ID> {

	private final NodeDescription<T> nodeDescription;
	private final BiFunction<TypeSystem, Record, T> mappingFunction;
	private final Expression idExpression;

	DefaultNeo4jEntityInformation(
		Neo4jPersistentEntity<T> entityMetaData,
		BiFunction<TypeSystem, Record, T> mappingFunction
	) {
		super(entityMetaData);

		this.nodeDescription = entityMetaData;
		this.mappingFunction = mappingFunction;

		this.idExpression = CypherAdapterUtils.createIdExpression(nodeDescription);
	}

	@Override
	public Expression getIdExpression() {
		return this.idExpression;
	}

	@Override
	public NodeDescription getNodeDescription() {
		return this.nodeDescription;
	}

	@Override
	public BiFunction<TypeSystem, Record, T> getMappingFunction() {
		return mappingFunction;
	}
}
