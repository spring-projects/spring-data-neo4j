/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.query;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.data.neo4j.template.GraphCallback;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
* @author mh
* @since 31.10.11
*/
abstract class GraphRepositoryQuery implements RepositoryQuery, ParameterResolver {
    private final GraphQueryMethod queryMethod;
    protected final Neo4jTemplate template;

    public GraphRepositoryQuery(GraphQueryMethod queryMethod, final Neo4jTemplate template) {
        Assert.notNull(queryMethod);
        Assert.notNull(template);
        this.queryMethod = queryMethod;
        this.template = template;
    }

    protected Neo4jTemplate getTemplate() {
        return template;
    }

    @Override
    public Map<Parameter, Object> resolveParameters(Map<Parameter, Object> parameters) {
        Map<Parameter, Object> result=new LinkedHashMap<Parameter, Object>();
        for (Map.Entry<Parameter, Object> entry : parameters.entrySet()) {
            result.put(entry.getKey(), convertGraphEntityToId(entry.getValue()));
        }
        return result;
    }

    private Object convertGraphEntityToId(Object value) {
        final Class<?> type = value.getClass();
        if (template.isNodeEntity(type)) {
            final Node state = template.getPersistentState(value);
            if (state != null) return state.getId();
        }
        if (template.isRelationshipEntity(type)) {
            final Relationship state = template.getPersistentState(value);
            if (state != null) return state.getId();
        }
        return value;
    }

    @Override
    public Object execute(final Object[] parameters) {
        return template.exec(new GraphCallback<Object>() {
            @Override
            public Object doWithGraph(GraphDatabase graph) throws Exception {
                final ParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);
                Map<String, Object> params = resolveParams(accessor);
                final String queryString = createQueryWithPagingAndSorting(accessor);
                return dispatchQuery(queryString, params, accessor);
            }
        });
    }

    protected Map<String, Object> resolveParams(ParameterAccessor accessor) {
        return queryMethod.resolveParams(accessor, this);
    }

    protected String createQueryWithPagingAndSorting(ParameterAccessor accessor) {
        return queryMethod.getQueryString();
    }

    @SuppressWarnings("unchecked")
    protected Object dispatchQuery(String queryString, Map<String, Object> params, ParameterAccessor accessor) {
        GraphQueryMethod queryMethod = getQueryMethod();
        final QueryEngine<?> queryEngine = getQueryEngine();
        final Class<?> compoundType = queryMethod.getCompoundType();
        if (queryMethod.isPageQuery() || queryMethod.isSliceQuery()) {
            @SuppressWarnings("unchecked") final Iterable<?> result = queryEngine.query(queryString, params).to(compoundType);
            Long count = computeCount(params);
            return createPage(result, accessor.getPageable(),count, queryMethod.isPageQuery());
        }
        if (queryMethod.isIterableResult()) {
            final Result<?> result = queryEngine.query(queryString, params).to(compoundType);
            if (queryMethod.isSetResult()) return IteratorUtil.addToCollection(result,new LinkedHashSet());
            if (queryMethod.isCollectionResult()) return IteratorUtil.addToCollection(result,new ArrayList());
            return result;
        }
        return queryEngine.query(queryString, params).to(queryMethod.getReturnType()).singleOrNull();
    }

    private Long computeCount(Map<String, Object> params) {
        String countQuery = queryMethod.getCountQueryString();
        if (countQuery == null || !StringUtils.hasText(countQuery)) return null;
        return getQueryEngine().query(countQuery,params).to(Long.class).singleOrNull();
    }

    @Override
    public GraphQueryMethod getQueryMethod() {
        return queryMethod;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object createPage(Iterable<?> result, Pageable pageable, Long count, boolean isPageQuery) {
        final List resultList = IteratorUtil.addToCollection(result, new ArrayList());
        if (pageable==null) {
            return isPageQuery ? new PageImpl(resultList) : new SliceImpl(resultList);
        }
        int pageSize = pageable.getPageSize();
        int requestedCountStart = pageable.getOffset();
        int resultSize = resultList.size();
        int currentTotal;
        if (count!=null) {
            currentTotal = count.intValue();
        } else {
            if (resultSize == pageSize) currentTotal = requestedCountStart + pageSize;
            else currentTotal = requestedCountStart + resultSize;
        }
        int resultWindowSize = Math.min(resultSize, pageSize);
        boolean hasNext = resultWindowSize < resultSize;
        List resultListPage = resultList.subList(0, resultWindowSize);

        return isPageQuery ?
                new PageImpl(resultListPage, pageable, currentTotal) :
                new SliceImpl(resultListPage,pageable, hasNext);
    }

    protected abstract CypherQueryEngine getQueryEngine();
}
