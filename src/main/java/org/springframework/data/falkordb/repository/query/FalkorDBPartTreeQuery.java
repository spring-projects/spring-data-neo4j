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
package org.springframework.data.falkordb.repository.query;

import java.util.Arrays;

import org.springframework.data.domain.Sort;
import org.springframework.data.falkordb.core.FalkorDBOperations;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBPersistentEntity;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * {@link RepositoryQuery} implementation for FalkorDB that handles derived queries by
 * parsing method names and generating appropriate Cypher queries.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class FalkorDBPartTreeQuery implements RepositoryQuery {

	private final FalkorDBQueryMethod queryMethod;

	private final FalkorDBOperations falkorDBOperations;

	private final PartTree partTree;

	private final DefaultFalkorDBPersistentEntity<?> entity;

	/**
	 * Creates a new {@link FalkorDBPartTreeQuery}.
	 * @param queryMethod must not be {@literal null}.
	 * @param falkorDBOperations must not be {@literal null}.
	 */
	public FalkorDBPartTreeQuery(FalkorDBQueryMethod queryMethod, FalkorDBOperations falkorDBOperations) {
		this.queryMethod = queryMethod;
		this.falkorDBOperations = falkorDBOperations;
		this.partTree = new PartTree(queryMethod.getName(),
				queryMethod.getResultProcessor().getReturnedType().getDomainType());
		this.entity = queryMethod.getMappingContext()
			.getRequiredPersistentEntity(queryMethod.getResultProcessor().getReturnedType().getDomainType());
	}

	@Override
	public Object execute(Object[] parameters) {
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(this.queryMethod.getParameters(),
				parameters);

		// Generate Cypher query from method name
		DerivedCypherQueryGenerator queryCreator = new DerivedCypherQueryGenerator(this.partTree, this.entity,
				this.queryMethod.getMappingContext());

		Sort sort = accessor.getSort();
		Object[] values = Arrays.stream(parameters).filter(param -> !(param instanceof Sort)).toArray();

		// Create query with parameters
		CypherQuery cypherQuery = queryCreator.createQuery(sort, values);

		// Execute the generated query
		Class<?> domainType = this.queryMethod.getResultProcessor().getReturnedType().getDomainType();

		if (this.partTree.isDelete()) {
			// Handle delete queries
			this.falkorDBOperations.query(cypherQuery.getQuery(), cypherQuery.getParameters(), domainType);
			return null;
		}
		else if (this.partTree.isCountProjection()) {
			// Handle count queries - modify query to return count
			String countQuery = cypherQuery.getQuery().replace("RETURN n", "RETURN count(n) as count");
			return this.falkorDBOperations.queryForObject(countQuery, cypherQuery.getParameters(), Long.class)
				.orElse(0L);
		}
		else if (this.partTree.isExistsProjection()) {
			// Handle exists queries - modify query to return boolean
			String existsQuery = cypherQuery.getQuery().replace("RETURN n", "RETURN count(n) > 0 as exists");
			return this.falkorDBOperations.queryForObject(existsQuery, cypherQuery.getParameters(), Boolean.class)
				.orElse(false);
		}
		else if (this.queryMethod.isCollectionQuery()) {
			// Handle collection returns (findBy...)
			return this.falkorDBOperations.query(cypherQuery.getQuery(), cypherQuery.getParameters(), domainType);
		}
		else {
			// Handle single entity returns (findOneBy...)
			return this.falkorDBOperations
				.queryForObject(cypherQuery.getQuery(), cypherQuery.getParameters(), domainType)
				.orElse(null);
		}
	}

	@Override
	public FalkorDBQueryMethod getQueryMethod() {
		return this.queryMethod;
	}

}
