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

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.session.Session;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.repository.query.GraphQueryMethod;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Specialisation of {@link RepositoryQuery} that handles mapping of derived finders.
 *
 * @author Mark Angrish
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Vince Bickers
 */
public class DerivedGraphRepositoryQuery implements RepositoryQuery {

	private DerivedQueryDefinition queryDefinition;

	private final GraphQueryMethod graphQueryMethod;
	private final PartTree tree;

	protected final Session session;
	final EntityMetadata info;

	private final int DEFAULT_QUERY_DEPTH = 1;

	public DerivedGraphRepositoryQuery(GraphQueryMethod graphQueryMethod, Session session) {
		this.graphQueryMethod = graphQueryMethod;
		this.session = session;
		this.info = graphQueryMethod.getEntityInformation();
		this.tree = new PartTree(graphQueryMethod.getName(), info.getJavaType());
		this.queryDefinition = new DerivedQueryCreator(tree, info.getJavaType()).createQuery();
	}

	@Override
	public Object execute(Object[] parameters) {
		return doExecute(parameters);
	}

	private Object doExecute(Object[] parameters) {
		if (tree.isCountProjection()) {
			return new CountByQuery().execute(parameters);
		}
		if (tree.isDelete()) {
			return new DeleteByQuery().execute(parameters);
		}
		return new FindByQuery().execute(parameters);

	}

	class CountByQuery implements RepositoryQuery {

		@Override
		public Object execute(Object[] parameters) {

			if (getQueryMethod().getReturnedObjectType().equals(Long.class)) {
				Filters filters;
				try {
					filters = resolveParams(parameters);
				} catch (EmptyStackException e) {
					throw new IllegalArgumentException("Not enough arguments for query " + getQueryMethod().getName());
				}
				return session.count(info.getJavaType(), filters);
			} else {
				throw new RuntimeException("Long is required as the return type of a Count query");
			}
		}

		@Override
		public QueryMethod getQueryMethod() {
			return graphQueryMethod;
		}
	}

	class DeleteByQuery implements RepositoryQuery {

		@Override
		public Object execute(Object[] parameters) {

			Filters filters;
			try {
				filters = resolveParams(parameters);
			} catch (EmptyStackException e) {
				throw new IllegalArgumentException("Not enough arguments for query " + getQueryMethod().getName());
			}

			Class<?> returnType = graphQueryMethod.resolveConcreteReturnType();

			if (returnType.equals(Long.class)) {
				if (graphQueryMethod.isCollectionQuery()) {
					return session.delete(info.getJavaType(), filters, true); // list deleted ids
				} else {
					return session.delete(info.getJavaType(), filters, false); // count deleted ids
				}
			}
			throw new RuntimeException("Long or Iterable<Long> is required as the return type of a Delete query");
		}

		@Override
		public QueryMethod getQueryMethod() {
			return graphQueryMethod;
		}
	}

	class FindByQuery implements RepositoryQuery {

		public Object execute(Object[] parameters) {

			ParameterAccessor accessor = new ParametersParameterAccessor(graphQueryMethod.getParameters(), parameters);
			Pageable pageable = accessor.getPageable();
			Sort sort = accessor.getSort();

			Class<?> returnType = graphQueryMethod.getMethod().getReturnType();
			Class<?> concreteType = graphQueryMethod.resolveConcreteReturnType();

			int queryDepth = calculateQueryDepth(parameters);

			Filters params;
			try {
				params = resolveParams(parameters);
			} catch (EmptyStackException e) {
				throw new IllegalArgumentException("Not enough arguments for query " + getQueryMethod().getName());
			}
			if (returnType.equals(Void.class)) {
				throw new RuntimeException("Derived Queries must have a return type");
			}

			if (Iterable.class.isAssignableFrom(returnType)) {
				PagingAndSorting pagingAndSorting = configurePagingAndSorting(pageable, sort);
				List resultList = queryResults(concreteType, queryDepth, params, pagingAndSorting);

				if (graphQueryMethod.isPageQuery() || graphQueryMethod.isSliceQuery()) {
					return createPage(graphQueryMethod, resultList, pageable);
				} else {
					return resultList;
				}
			}

			Iterator<?> objectIterator = session.loadAll(returnType, params, queryDepth).iterator();
			if (objectIterator.hasNext()) {
				return objectIterator.next();
			}
			return null;
		}

		@Override
		public QueryMethod getQueryMethod() {
			return graphQueryMethod;
		}

		private List queryResults(Class<?> concreteType, int queryDepth, Filters params,
				PagingAndSorting pagingAndSorting) {
			List resultList;
			switch (pagingAndSorting.configuration()) {
				case PagingAndSorting.PAGING_AND_SORTING:
					resultList = (List) session.loadAll(concreteType, params, pagingAndSorting.sortOrder,
							pagingAndSorting.pagination, queryDepth);
					break;

				case PagingAndSorting.PAGING_ONLY:
					resultList = (List) session.loadAll(concreteType, params, pagingAndSorting.pagination, queryDepth);
					break;

				case PagingAndSorting.SORTING_ONLY:
					resultList = (List) session.loadAll(concreteType, params, pagingAndSorting.sortOrder, queryDepth);
					break;

				case PagingAndSorting.NO_PAGING_OR_SORTING:
					resultList = (List) session.loadAll(concreteType, params, queryDepth);
					break;

				default:
					resultList = (List) session.loadAll(concreteType, params, queryDepth);
			}
			return resultList;
		}

		private int calculateQueryDepth(Object[] parameters) {
			int queryDepth = DEFAULT_QUERY_DEPTH;
			if (graphQueryMethod.hasStaticDepth()) {
				queryDepth = graphQueryMethod.getQueryDepth();
			} else {
				if (graphQueryMethod.getQueryDepthParamIndex() != null) {
					queryDepth = (int) parameters[graphQueryMethod.getQueryDepthParamIndex()];
				}
			}
			return queryDepth;
		}
	}

	@Override
	public QueryMethod getQueryMethod() {
		return graphQueryMethod;
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

	protected Object createPage(GraphQueryMethod graphQueryMethod, List resultList, Pageable pageable) {
		if (pageable == null) {
			return graphQueryMethod.isPageQuery() ? new PageImpl(resultList) : new SliceImpl(resultList);
		}
		int currentTotal = pageable.getOffset() + resultList.size()
				+ (resultList.size() == pageable.getPageSize() ? pageable.getPageSize() : 0);

		int resultWindowSize = Math.min(resultList.size(), pageable.getPageSize());
		boolean hasNext = resultWindowSize < resultList.size();
		List resultListPage = resultList.subList(0, resultWindowSize);

		return graphQueryMethod.isPageQuery() ? new PageImpl(resultListPage, pageable, currentTotal)
				: new SliceImpl(resultListPage, pageable, hasNext);
	}

	private SortOrder convert(Sort sort) {

		SortOrder sortOrder = new SortOrder();

		if (sort != null) {
			for (Sort.Order order : sort) {
				if (order.isAscending()) {
					sortOrder.add(order.getProperty());
				} else {
					sortOrder.add(SortOrder.Direction.DESC, order.getProperty());
				}
			}
		}
		return sortOrder;
	}

	private PagingAndSorting configurePagingAndSorting(Pageable pageable, Sort sort) {
		SortOrder sortOrder = null;
		Pagination pagination = null;

		if (pageable != null) {
			pagination = new Pagination(pageable.getPageNumber(), pageable.getPageSize());
			if (pageable.getSort() != null) {
				sortOrder = convert(pageable.getSort());
			}
		}
		if (sort != null) {
			sortOrder = convert(sort);
		}

		if (graphQueryMethod.isPageQuery() || graphQueryMethod.isSliceQuery()) {
			if (graphQueryMethod.isSliceQuery()) {
				pagination = new Pagination(pageable.getPageNumber(), pageable.getPageSize() + 1);
				pagination.setOffset(pageable.getPageNumber() * pageable.getPageSize()); // For a slice, need one extra result
																																									// to determine if there is a next
																																									// page
			} else {
				pagination = new Pagination(pageable.getPageNumber(), pageable.getPageSize());
			}
		}

		PagingAndSorting pagingAndSorting = new PagingAndSorting(pagination, sortOrder);
		return pagingAndSorting;
	}

	class PagingAndSorting {

		static final int PAGING_AND_SORTING = 0;
		static final int PAGING_ONLY = 1;
		static final int SORTING_ONLY = 2;
		static final int NO_PAGING_OR_SORTING = 3;

		Pagination pagination;
		SortOrder sortOrder;

		public PagingAndSorting(Pagination pagination, SortOrder sortOrder) {
			this.pagination = pagination;
			this.sortOrder = sortOrder;
		}

		int configuration() {
			if (pagination != null && sortOrder != null) {
				return PAGING_AND_SORTING;
			}
			if (pagination != null && sortOrder == null) {
				return PAGING_ONLY;
			}
			if (pagination == null && sortOrder != null) {
				return SORTING_ONLY;
			}
			return NO_PAGING_OR_SORTING;
		}
	}

}
