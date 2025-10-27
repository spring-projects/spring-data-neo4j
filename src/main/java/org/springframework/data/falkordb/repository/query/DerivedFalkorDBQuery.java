/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.repository.query;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.falkordb.core.FalkorDBOperations;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBPersistentEntity;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * {@link RepositoryQuery} implementation that executes derived queries based on method
 * names.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class DerivedFalkorDBQuery implements RepositoryQuery {

	/**
	 * The query method.
	 */
	private final FalkorDBQueryMethod queryMethod;

	/**
	 * The FalkorDB operations.
	 */
	private final FalkorDBOperations operations;

	/**
	 * The query generator.
	 */
	private final DerivedCypherQueryGenerator queryGenerator;

	/**
	 * The part tree for the query.
	 */
	private final PartTree partTree;

	/**
	 * Creates a new {@link DerivedFalkorDBQuery}.
	 * @param method must not be {@literal null}.
	 * @param falkorDBOperations must not be {@literal null}. 
	 */
	public DerivedFalkorDBQuery(final FalkorDBQueryMethod method, final FalkorDBOperations falkorDBOperations) {
		this.queryMethod = method;
		this.operations = falkorDBOperations;
		this.partTree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());

		DefaultFalkorDBPersistentEntity<?> entity = (DefaultFalkorDBPersistentEntity<?>) method.getMappingContext()
			.getRequiredPersistentEntity(method.getEntityInformation().getJavaType());

		this.queryGenerator = new DerivedCypherQueryGenerator(partTree, entity, method.getMappingContext());
	}

	@Override
	public Object execute(final Object[] parameters) {

		// Extract Sort parameter if present
		Sort sort = Sort.unsorted();
		Object[] queryParameters = parameters;

		if (parameters != null && parameters.length > 0) {
			Object lastParam = parameters[parameters.length - 1];
			if (lastParam instanceof Sort) {
				sort = (Sort) lastParam;
				// Remove Sort from parameters array
				queryParameters = new Object[parameters.length - 1];
				System.arraycopy(parameters, 0, queryParameters, 0, parameters.length - 1);
			}
		}

		// Generate the Cypher query
		CypherQuery cypherQuery = queryGenerator.createQuery(sort, queryParameters);

		ResultProcessor processor = queryMethod.getResultProcessor();
		ReturnedType returnedType = processor.getReturnedType();

		// Handle delete queries
		if (partTree.isDelete()) {
			operations.query(cypherQuery.getQuery(), cypherQuery.getParameters(), Object.class);
			return null;
		}

		// Handle count queries
		if (partTree.isCountProjection()) {
			List<Long> results = operations.query(cypherQuery.getQuery(), cypherQuery.getParameters(), Long.class);
			Long count = results.isEmpty() ? 0L : results.get(0);
			return processor.processResult(count);
		}

		// Handle exists queries
		if (partTree.isExistsProjection()) {
			List<Boolean> results = operations.query(cypherQuery.getQuery(), cypherQuery.getParameters(),
					Boolean.class);
			Boolean exists = results.isEmpty() ? false : results.get(0);
			return processor.processResult(exists);
		}

		// Handle collection queries
		if (queryMethod.isCollectionQuery()) {
			return processor
				.processResult(operations.query(cypherQuery.getQuery(), cypherQuery.getParameters(),
						returnedType.getDomainType()));
		}

		// Single result query
		Optional<?> result = operations.queryForObject(cypherQuery.getQuery(), cypherQuery.getParameters(),
				returnedType.getDomainType());
		return processor.processResult(result.orElse(null));
	}

	@Override
	public FalkorDBQueryMethod getQueryMethod() {
		return queryMethod;
	}

}
