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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;

/**
 * @author mh
 * @since 31.10.11
 */
class CypherGraphRepositoryQuery extends GraphRepositoryQuery {

    private CypherQueryEngine queryEngine;

    public CypherGraphRepositoryQuery(GraphQueryMethod queryMethod, final Neo4jTemplate template) {
        super(queryMethod, template);
    }

    @Override
    protected CypherQueryEngine getQueryEngine() {
        if (this.queryEngine != null) return this.queryEngine;
        this.queryEngine = getTemplate().queryEngineFor();
        return this.queryEngine;
    }

    private String addPaging(String baseQuery, Pageable pageable) {
        if (pageable==null) {
            return baseQuery;
        }
        return baseQuery + " skip "+pageable.getOffset() + " limit " + pageable.getPageSize() + 1 ;
    }

    private String addSorting(String baseQuery, Sort sort) {
        if (sort==null)
        {
            return baseQuery; // || sort.isEmpty()
        }
        final String sortOrder = getSortOrder(sort);
        if (sortOrder.isEmpty()) {
            return baseQuery;
        }
        return baseQuery + " order by " + sortOrder;
    }

    private String getSortOrder(Sort sort) {
        String result = "";
        for (Sort.Order order : sort) {
            if (!result.isEmpty()) result += ", ";
            result += order.getProperty() + " " + order.getDirection();
        }
        return result;
    }

    protected String createQueryWithPagingAndSorting(final ParameterAccessor accessor) {
        final GraphQueryMethod queryMethod = getQueryMethod();
        final Parameters<?, ?> parameters = queryMethod.getParameters();
        String queryString = queryMethod.getQueryString();
        if (parameters.hasSortParameter()) {
            queryString = addSorting(queryString, accessor.getSort());
        }
        if (parameters.hasPageableParameter()) {
            final Pageable pageable = accessor.getPageable();
            if (pageable!=null) {
                queryString = addSorting(queryString, pageable.getSort());
                queryString = addPaging(queryString, pageable);
            }
        }
        return queryString;
    }
}
