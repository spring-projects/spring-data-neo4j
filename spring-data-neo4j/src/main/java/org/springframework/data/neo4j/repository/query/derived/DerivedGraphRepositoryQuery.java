/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.repository.query.derived;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.repository.query.AbstractGraphRepositoryQuery;
import org.springframework.data.neo4j.repository.query.GraphParameterAccessor;
import org.springframework.data.neo4j.repository.query.GraphParametersParameterAccessor;
import org.springframework.data.neo4j.repository.query.GraphQueryMethod;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Specialisation of {@link RepositoryQuery} that handles mapping of derived finders.
 *
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Vince Bickers
 * @author Nicolas Mervaillie
 * @author Mark Paluch
 */
public class DerivedGraphRepositoryQuery extends AbstractGraphRepositoryQuery {

	private static final Logger LOG = LoggerFactory.getLogger(DerivedGraphRepositoryQuery.class);
	private final DerivedQueryDefinition queryDefinition;
	private final GraphQueryMethod graphQueryMethod;
	private final PartTree tree;

	public DerivedGraphRepositoryQuery(GraphQueryMethod graphQueryMethod, Session session) {
		super(graphQueryMethod, session);
		this.graphQueryMethod = graphQueryMethod;
		Class<?> domainType = graphQueryMethod.getEntityInformation().getJavaType();
		this.tree = new PartTree(graphQueryMethod.getName(), domainType);
		this.queryDefinition = new DerivedQueryCreator(tree, domainType).createQuery();
	}

	@Override
	protected Object doExecute(Query params, Object[] parameters) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query for method {}", graphQueryMethod.getName());
		}

		GraphParameterAccessor accessor = new GraphParametersParameterAccessor(graphQueryMethod, parameters);
		Class<?> returnType = graphQueryMethod.getMethod().getReturnType();

		if (returnType.equals(Void.class)) {
			throw new RuntimeException("Derived Queries must have a return type");
		}

		ResultProcessor processor = graphQueryMethod.getResultProcessor().withDynamicProjection(accessor);
		Object results = getExecution(accessor).execute(params, processor.getReturnedType().getDomainType());

		return processor.processResult(results);
	}

	@Override
	protected Query getQuery(Object[] parameters) {
		return new Query(resolveParams(parameters));
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

	/**
	 * Sets values from parameters supplied by the finder on {@link org.neo4j.ogm.cypher.Filter} built by the
	 * {@link GraphQueryMethod}
	 *
	 * @param parameters parameter values supplied by the finder method
	 * @return List of Parameter with values set
	 */
	private Filters resolveParams(Object[] parameters) {
		Map<Integer, Object> params = new HashMap<>();

		for (int i = 0; i < parameters.length; i++) {
			if (graphQueryMethod.getQueryDepthParamIndex() == null
					|| (graphQueryMethod.getQueryDepthParamIndex() != null && graphQueryMethod.getQueryDepthParamIndex() != i)) {
				params.put(i, parameters[i]);
			}
		}

		return new Filters(queryDefinition.getFilters(params));
	}
}
