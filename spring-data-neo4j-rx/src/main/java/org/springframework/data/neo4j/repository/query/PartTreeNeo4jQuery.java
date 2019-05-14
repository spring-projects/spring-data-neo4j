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

import static java.util.stream.Collectors.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Range;
import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.query.Neo4jQueryMethod.Neo4jParameters;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Implementation of {@link RepositoryQuery} for derived finder methods.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
final class PartTreeNeo4jQuery extends AbstractNeo4jQuery {

	private final ResultProcessor processor;
	private final PartTree tree;

	PartTreeNeo4jQuery(
		NodeManager nodeManager,
		Neo4jMappingContext mappingContext,
		Neo4jQueryMethod queryMethod
	) {
		super(nodeManager, mappingContext, queryMethod);

		this.processor = queryMethod.getResultProcessor();
		this.tree = new PartTree(queryMethod.getName(), domainType);
	}

	@Override
	protected PreparedQuery<?> prepareQuery(Object[] parameters) {

		Neo4jParameters formalParameters = (Neo4jParameters) this.queryMethod.getParameters();
		ParameterAccessor actualParameters = new ParametersParameterAccessor(formalParameters, parameters);
		CypherQueryCreator queryCreator = new CypherQueryCreator(
			mappingContext, domainType, tree, formalParameters, actualParameters
		);

		String cypherQuery = queryCreator.createQuery();
		Map<String, Object> boundedParameters = formalParameters
			.getBindableParameters().stream()
			.collect(toMap(Neo4jQueryMethod.Neo4jParameter::getNameOrIndex,
				formalParameter -> convertParameter(parameters[formalParameter.getIndex()])));

		return PreparedQuery.queryFor(super.domainType).withCypherQuery(cypherQuery)
			.withParameters(boundedParameters)
			.usingMappingFunction(mappingContext.getMappingFunctionFor(super.domainType).orElse(null))
			.build();
	}

	// TODO Have fun with a bunch of conversion services
	static Object convertParameter(Object parameter) {
		if (parameter instanceof Range) {
			Range range = (Range) parameter;
			Map<String, Object> map = new HashMap<>();
			range.getLowerBound().getValue().ifPresent(v -> map.put("lb", v));
			range.getUpperBound().getValue().ifPresent(v -> map.put("ub", v));
			return map;
		}
		return parameter;
	}

	@Override
	protected boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	protected boolean isExistsQuery() {
		return tree.isExistsProjection();
	}

	@Override
	protected boolean isDeleteQuery() {
		return tree.isDelete();
	}

	@Override
	protected boolean isLimiting() {
		return tree.isLimiting();
	}
}
