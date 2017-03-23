/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.query;


import static java.lang.reflect.Proxy.newProxyInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.metadata.reflect.EntityFactory;
import org.neo4j.ogm.request.Request;
import org.neo4j.ogm.session.GraphCallback;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.transaction.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;

/**
 * Specialisation of {@link GraphRepositoryQuery} that handles mapping to objects annotated with <code>&#064;QueryResult</code>.
 *
 * @author Adam George
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Mark Paluch
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
		if (pageable.isPaged()) {
			sort = pageable.getSort();
		}
		if (sort != Sort.unsorted()) {
			//Custom queries in the OGM do not support pageable
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

	private List mappedConcreteResults(final Class<?> targetType, final String query, final Map<String, Object> queryParams) {
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
		Class<?>[] interfaces = new Class<?>[]{targetType};
		for (Map<String, Object> map : queryResults) {
			resultObjects.add(newProxyInstance(targetType.getClassLoader(), interfaces, new QueryResultProxy(map)));
		}
		return resultObjects;
	}
}
