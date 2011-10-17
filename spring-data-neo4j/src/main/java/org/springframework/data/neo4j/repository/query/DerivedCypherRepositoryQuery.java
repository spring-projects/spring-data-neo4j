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

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.repository.GraphRepositoryFactory.GraphQueryMethod;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

/**
 * {@link RepositoryQuery} implementation that derives a Cypher query from the {@link GraphQueryMethod}'s method name.
 * 
 * @author Oliver Gierke
 */
public class DerivedCypherRepositoryQuery implements RepositoryQuery {

    private final GraphQueryMethod method;
    private final CypherQueryExecutor executor;
    private final CypherQueryDefinition query;

    /**
     * Creates a new {@link DerivedCypherRepositoryQuery} from the given {@link MappingContext},
     * {@link GraphQueryMethod} and {@link org.springframework.data.neo4j.support.Neo4jTemplate}.
     * 
     * @param context must not be {@literal null}.
     * @param method must not be {@literal null}.
     * @param database must not be {@literal null}.
     */
    public DerivedCypherRepositoryQuery(MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context, GraphQueryMethod method, Neo4jTemplate database) {

        Assert.notNull(context);
        Assert.notNull(method);
        Assert.notNull(database);

        EntityMetadata<?> info = method.getEntityInformation();
        PartTree tree = new PartTree(method.getName(), info.getJavaType());

        this.query = new CypherQueryCreator(tree, context, info.getJavaType()).createQuery();
        this.method = method;
        this.executor = new CypherQueryExecutor(database.queryEngineFor(QueryType.Cypher));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
     */
    @Override
    public Object execute(Object[] parameters) {

        ParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), parameters);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        int counter = 0;

        for (Object parameter : accessor) {
            paramMap.put(String.format(QueryTemplates.PARAMETER, counter++), parameter);
        }

        Class<?> type = method.getEntityInformation().getJavaType();
        String query = getQuery(this.query, accessor);

        if (method.isCollectionQuery()) {
            return executor.query(query, type, paramMap);
        } else {
            return executor.queryForObject(query, type, paramMap);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
     */
    @Override
    public QueryMethod getQueryMethod() {
        return method;
    }

    /**
     * Returns the actual Cypher query applying {@link Pageable} or {@link Sort} instances.
     * 
     * @param query
     * @param accessor
     * @return
     */
    private String getQuery(CypherQueryDefinition query, ParameterAccessor accessor) {

        if (accessor.getPageable() != null) {
            return query.toString(accessor.getPageable());
        } else if (accessor.getSort() != null) {
            return query.toString(accessor.getSort());
        } else {
            return query.toString();
        }
    }
}
