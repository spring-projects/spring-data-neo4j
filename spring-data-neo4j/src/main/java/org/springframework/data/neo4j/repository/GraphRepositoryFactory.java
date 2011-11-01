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

package org.springframework.data.neo4j.repository;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.repository.query.DerivedCypherRepositoryQuery;
import org.springframework.data.neo4j.repository.query.QueryTemplates;
import org.springframework.data.neo4j.support.GenericTypeExtractor;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.*;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 28.03.11
 */
public class GraphRepositoryFactory extends RepositoryFactorySupport {

    private final Neo4jTemplate template;
    private final MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext;

    /**
     * Creates a new {@link GraphRepositoryFactory} from the given {@link org.springframework.data.neo4j.support.Neo4jTemplate} and
     * {@link MappingContext}.
     * 
     * @param template must not be {@literal null}.
     * @param mappingContext must not be {@literal null}.
     */
    public GraphRepositoryFactory(Neo4jTemplate template, MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {

        Assert.notNull(template);
        Assert.notNull(mappingContext);

        this.template = template;
        this.mappingContext = mappingContext;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.repository.support.RepositoryFactorySupport#
     * getTargetRepository(java.lang.Class)
     */
    @Override
    protected Object getTargetRepository(RepositoryMetadata metadata) {
        return getTargetRepository(metadata, template);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object getTargetRepository(RepositoryMetadata metadata, Neo4jTemplate template) {

        Class<?> type = metadata.getDomainClass();
        GraphEntityInformation entityInformation = (GraphEntityInformation)getEntityInformation(type);
        // todo entityInformation.isGraphBacked();
        if (entityInformation.isNodeEntity()) {
            return new NodeGraphRepository(type, template);
        } else {
            return new RelationshipGraphRepository(type, template);
        }
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        Class<?> domainClass = repositoryMetadata.getDomainClass();

        @SuppressWarnings("rawtypes")
        final GraphEntityInformation entityInformation = (GraphEntityInformation) getEntityInformation(domainClass);
        if (entityInformation.isNodeEntity()) {
            return NodeGraphRepository.class;
        }
        if (entityInformation.isRelationshipEntity()) {
            return RelationshipGraphRepository.class;
        }
        throw new IllegalArgumentException("Invalid Domain Class "+ domainClass+" neither Node- nor RelationshipEntity");
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> type) {
        return new GraphMetamodelEntityInformation(type, template);
    }



    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key) {
        return new QueryLookupStrategy() {
            @Override
            public RepositoryQuery resolveQuery(Method method, RepositoryMetadata repositoryMetadata, NamedQueries namedQueries) {
                final GraphQueryMethod queryMethod = new GraphQueryMethod(method, repositoryMetadata,namedQueries);

                if (!queryMethod.hasAnnotation() && !namedQueries.hasQuery(queryMethod.getNamedQueryName())) {
                    return new DerivedCypherRepositoryQuery(mappingContext, queryMethod, template);
                }

                return queryMethod.createQuery(repositoryMetadata, GraphRepositoryFactory.this.template);
            }
        };
    }

    public static class GraphQueryMethod extends QueryMethod {

        private final Method method;
        private final Query queryAnnotation;
        private final String query;

        public GraphQueryMethod(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {
            super(method, metadata);
            this.method = method;
            queryAnnotation = method.getAnnotation(Query.class);
            this.query = queryAnnotation != null ? queryAnnotation.value() : getNamedQuery(namedQueries);
        }

        public boolean isValid() {
            return this.query!=null; // && this.compoundType != null
        }

        private String getNamedQuery(NamedQueries namedQueries) {
            final String namedQueryName = getNamedQueryName();
            if (namedQueries.hasQuery(namedQueryName)) {
                return namedQueries.getQuery(namedQueryName);
            }
            return null;
        }

        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        private String prepareQuery(Object[] args) {
            final Parameters parameters = getParameters();
            String queryString = this.query;
            if (parameters.hasSortParameter()) {
                queryString = addSorting(queryString, (Sort) args[parameters.getSortIndex()]);
            }
            if (parameters.hasPageableParameter()) {
                final Pageable pageable = getPageable(args);
                if (pageable!=null) {
                    queryString = addSorting(queryString, pageable.getSort());
                    queryString = addPaging(queryString, pageable);
                }
            }
            return queryString;
        }

        private Map<String, Object> resolveParams(Object[] parameters, Neo4jTemplate template) {
            Map<String, Object> params = new HashMap<String, Object>();
            for (Parameter parameter : getParameters().getBindableParameters()) {
                final Object value = parameters[parameter.getIndex()];
                final String parameterName = parameter.getName();
                if (parameterName != null) params.put(parameterName, resolveParameter(value, template));
                else params.put(String.format(QueryTemplates.PARAMETER, parameter.getIndex()), resolveParameter(value, template));
            }
            return params;
        }

        private Pageable getPageable(Object[] args) {
            Parameters parameters = getParameters();
            if (parameters.hasPageableParameter()) {
                return (Pageable) args[parameters.getPageableIndex()];
            }
            return null;
        }

        private String addPaging(String baseQuery, Pageable pageable) {
            if (pageable==null) {
                return baseQuery;
            }
            return baseQuery + " skip "+pageable.getOffset() + " limit " + pageable.getPageSize();
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
                result += order.getProperty() + " " + order.getDirection();
            }
            return result;
        }

        private Object resolveParameter(Object parameter, Neo4jTemplate template) {
            final Class<?> type = parameter.getClass();
            if (template.isNodeEntity(type)) {
                final Node state = template.getPersistentState(parameter);
                if (state != null) return state.getId();
            }
            if (template.isRelationshipEntity(type)) {
                final Relationship state = template.getPersistentState(parameter);
                if (state != null) return state.getId();
            }
            return parameter;
        }

        private Class<?> getCompoundType() {
            final Class<?> elementClass = getElementClass();
            if (elementClass!=null) {
                return elementClass;
            }
            return GenericTypeExtractor.resolveReturnedType(method);
        }

        private Class<?> getElementClass() {
            if (!hasAnnotation() || queryAnnotation.elementClass().equals(Object.class)) {
                return null;
            }
            return queryAnnotation.elementClass();
        }

        public String getQueryString() {
            return this.query;
        }

        public boolean hasAnnotation() {
            return queryAnnotation!=null;
        }

        private boolean isIterableResult() {
            return Iterable.class.isAssignableFrom(getReturnType());
        }

        private RepositoryQuery createQuery(RepositoryMetadata repositoryMetadata, final Neo4jTemplate context) {
            if (!isValid()) {
                return null;
            }
            if (queryAnnotation == null) {
                return new CypherGraphRepositoryQuery(this, repositoryMetadata, context); // cypher is default for named queries
            }
            switch (queryAnnotation.type()) {
            case Cypher:
                return new CypherGraphRepositoryQuery(this, repositoryMetadata, context);
            case Gremlin:
                return new GremlinGraphRepositoryQuery(this, repositoryMetadata, context);
            default:
                throw new IllegalStateException("@Query Annotation has to be configured as Cypher or Gremlin Query");
            }
        }
    }


    private static class CypherGraphRepositoryQuery extends GraphRepositoryQuery {

        private CypherQueryExecutor queryExecutor;

        public CypherGraphRepositoryQuery(GraphQueryMethod queryMethod, RepositoryMetadata metadata, final Neo4jTemplate template) {
            super(queryMethod, metadata, template);
        }

        private CypherQueryExecutor getQueryExecutor() {
            if (this.queryExecutor!=null) return this.queryExecutor;
            this.queryExecutor = new CypherQueryExecutor(getTemplate().queryEngineFor(QueryType.Cypher));
            return this.queryExecutor;
        }

        @Override
        protected Object dispatchQuery(String queryString, Map<String, Object> params, Pageable pageable) {
            GraphQueryMethod queryMethod = getQueryMethod();
            final Class<?> compoundType = queryMethod.getCompoundType();
            if (queryMethod.isPageQuery()) {
                return queryPaged(queryString,params,pageable);
            }
            if (queryMethod.isIterableResult()) {
                if (compoundType.isAssignableFrom(Map.class)) {
                    return getQueryExecutor().queryForList(queryString, params);
                }
                return getQueryExecutor().query(queryString, queryMethod.getCompoundType(), params);
            }
            return getQueryExecutor().queryForObject(queryString, queryMethod.getReturnType(), params);
        }
        private Object queryPaged(String queryString, Map<String, Object> params, Pageable pageable) {
            final Iterable<?> result = getQueryExecutor().query(queryString, getQueryMethod().getCompoundType(), params);
            return createPage(result, pageable);
        }
    }

    private static class GremlinGraphRepositoryQuery extends GraphRepositoryQuery {

        private QueryEngine queryEngine;

        public GremlinGraphRepositoryQuery(GraphQueryMethod queryMethod, RepositoryMetadata metadata, final Neo4jTemplate template) {
            super(queryMethod, metadata, template);
        }

        private QueryEngine getQueryEngine() {
            if (this.queryEngine !=null) return queryEngine;
            this.queryEngine = getTemplate().queryEngineFor(QueryType.Gremlin);
            return this.queryEngine;
        }


        @SuppressWarnings("unchecked")
        @Override
        protected Object dispatchQuery(String queryString, Map<String, Object> params, Pageable pageable) {
            GraphQueryMethod queryMethod = getQueryMethod();
            if (queryMethod.isPageQuery()) {
                return queryPaged(queryString,params,pageable);
            }
            if (queryMethod.isIterableResult()) {
                return getQueryEngine().query(queryString, params).to(queryMethod.getCompoundType());
            }
            return getQueryEngine().query(queryString, params).to(queryMethod.getReturnType()).single();
        }

        private Object queryPaged(String queryString, Map<String, Object> params, Pageable pageable) {
            @SuppressWarnings("unchecked") final Iterable<?> result = getQueryEngine().query(queryString, params).to(getQueryMethod().getCompoundType());
            return createPage(result, pageable);
        }

    }

    private static abstract class GraphRepositoryQuery implements RepositoryQuery {
        private final GraphQueryMethod queryMethod;
        private final Neo4jTemplate template;

        public GraphRepositoryQuery(GraphQueryMethod queryMethod, RepositoryMetadata metadata, final Neo4jTemplate template) {
            this.queryMethod = queryMethod;
            this.template = template;
        }

        protected Neo4jTemplate getTemplate() {
            return template;
        }

        @Override
        public Object execute(Object[] parameters) {
            Map<String, Object> params = queryMethod.resolveParams(parameters, template);
            final String queryString = queryMethod.prepareQuery(parameters);
            return dispatchQuery(queryString,params,queryMethod.getPageable(parameters));
        }

        protected abstract Object dispatchQuery(String queryString, Map<String, Object> params, Pageable pageable);

        @Override
        public GraphQueryMethod getQueryMethod() {
            return queryMethod;
        }


        @SuppressWarnings({"unchecked", "rawtypes"})
        protected Object createPage(Iterable<?> result, Pageable pageable) {
            final List resultList = IteratorUtil.addToCollection(result, new ArrayList());
            if (pageable==null) {
                return new PageImpl(resultList);
            }
            final int currentTotal = pageable.getOffset() + pageable.getPageSize();
            return new PageImpl(resultList, pageable, currentTotal);
        }
    }
}