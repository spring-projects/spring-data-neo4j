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

package org.springframework.data.neo4j.repository.query;

import static java.lang.reflect.Proxy.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.entity.io.EntityFactory;
import org.neo4j.ogm.request.Request;
import org.neo4j.ogm.session.GraphCallback;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.transaction.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;

/**
 * Specialisation of {@link GraphRepositoryQuery} that handles mapping to objects annotated with
 * <code>&#064;QueryResult</code>.
 *
 * @author Adam George
 * @author Luanne Misquitta
 * @author Jasper Blues
 */
public class QueryResultGraphRepositoryQuery extends GraphRepositoryQuery {

	/**
	 * Constructs a new {@link QueryResultGraphRepositoryQuery} based on the given arguments.
	 *
	 * @param graphQueryMethod The {@link GraphQueryMethod} to which this repository query corresponds
	 * @param session The OGM {@link Session} used to execute the query
	 */
	public QueryResultGraphRepositoryQuery(GraphQueryMethod graphQueryMethod, Session session) {
		super(graphQueryMethod, session);
	}

	@Override
	protected Object execute(Class<?> returnType, final Class<?> concreteReturnType, String cypherQuery,
			Map<String, Object> queryParams, ParameterAccessor parameterAccessor) {

		Pageable pageable = parameterAccessor.getPageable();
		Sort sort = parameterAccessor.getSort();
		if (pageable != null && pageable.getSort() != null) {
			sort = pageable.getSort();
		}
		if (sort != null) {
			// Custom queries in the OGM do not support pageable
			cypherQuery = addSorting(cypherQuery, sort);
		}

		Object resultObjects = concreteReturnType.isInterface()
				? mapToProxy(concreteReturnType, cypherQuery, queryParams, pageable)
				: mapToConcreteType(concreteReturnType, cypherQuery, queryParams, pageable);

		if (Iterable.class.isAssignableFrom(returnType)) {
			return resultObjects;
		} else {
			Collection<Object> objects = (Collection<Object>) resultObjects;
			return objects.isEmpty() ? null : objects.iterator().next();
		}
	}

	private Object mapToConcreteType(final Class<?> targetType, final String cypherQuery,
			final Map<String, Object> queryParams, Pageable pageable) {

		if (graphQueryMethod.isPageQuery() || graphQueryMethod.isSliceQuery()) {
			final String query = addPaging(cypherQuery, queryParams, pageable.getPageNumber(), pageable.getPageSize());
			List resultList = mappedConcreteResults(targetType, query, queryParams);
			return createPage(graphQueryMethod, resultList, pageable, computeCount(queryParams));
		} else {
			return mappedConcreteResults(targetType, cypherQuery, queryParams);
		}
	}

	private Object mapToProxy(Class<?> targetType, String cypherQuery, Map<String, Object> queryParams,
			Pageable pageable) {
		if (graphQueryMethod.isPageQuery() || graphQueryMethod.isSliceQuery()) {
			final String query = addPaging(cypherQuery, queryParams, pageable.getPageNumber(), pageable.getPageSize());
			List<Object> objects = mappedProxyResults(targetType, this.session.query(query, queryParams));
			return createPage(graphQueryMethod, objects, pageable, computeCount(queryParams));
		} else {
			return mappedProxyResults(targetType, this.session.query(cypherQuery, queryParams));
		}
	}

	private List mappedConcreteResults(final Class<?> targetType, final String query,
			final Map<String, Object> queryParams) {
		return (List) this.session.doInTransaction(new GraphCallback<Collection<Object>>() {
			@Override
			public Collection<Object> apply(Request requestHandler, Transaction transaction, MetaData metaData) {
				Collection<Object> toReturn = new ArrayList<>();
				SingleUseEntityMapper entityMapper = new SingleUseEntityMapper(metaData, new EntityFactory(metaData));
				Iterable<Map<String, Object>> results = session.query(query, queryParams);
				for (Map<String, Object> result : results) {
					toReturn.add(entityMapper.map(targetType, result));
				}
				return toReturn;
			}
		});
	}

	private List mappedProxyResults(Class<?> targetType, Iterable<Map<String, Object>> queryResults) {
		List<Object> resultObjects = new ArrayList<>();
		Class<?>[] interfaces = new Class<?>[] { targetType };
		for (Map<String, Object> map : queryResults) {
			resultObjects.add(newProxyInstance(targetType.getClassLoader(), interfaces, new QueryResultProxy(map)));
		}
		return resultObjects;
	}
}
