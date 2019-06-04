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
package org.neo4j.springframework.data.repository.support;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.cypher.Expression;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.repository.query.CypherAdapterUtils;
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

	private final Neo4jPersistentEntity<T> entityMetaData;
	private final BiFunction<TypeSystem, Record, T> mappingFunction;
	private final Function<T, Map<String, Object>> binderFunction;

	private final Expression idExpression;

	DefaultNeo4jEntityInformation(
		Neo4jPersistentEntity<T> entityMetaData,
		BiFunction<TypeSystem, Record, T> mappingFunction,
		Function<T, Map<String, Object>> binderFunction
	) {
		super(entityMetaData);

		this.entityMetaData = entityMetaData;
		this.mappingFunction = mappingFunction;
		this.binderFunction = binderFunction;

		this.idExpression = CypherAdapterUtils.createIdExpression(entityMetaData);
	}

	/*
	 * (non-Javadoc)
	 * @see Neo4jEntityInformation#getIdExpression()
	 */
	@Override
	public Expression getIdExpression() {
		return this.idExpression;
	}

	/*
	 * (non-Javadoc)
	 * @see Neo4jEntityInformation#getEntityMetaData()
	 */
	@Override
	public Neo4jPersistentEntity<T> getEntityMetaData() {
		return this.entityMetaData;
	}

	/*
	 * (non-Javadoc)
	 * @see Neo4jEntityInformation#getMappingFunction()
	 */
	@Override
	public BiFunction<TypeSystem, Record, T> getMappingFunction() {
		return mappingFunction;
	}

	/*
	 * (non-Javadoc)
	 * @see Neo4jEntityInformation#getBinderFunction()
	 */
	@Override
	public Function<T, Map<String, Object>> getBinderFunction() {
		return binderFunction;
	}
}
