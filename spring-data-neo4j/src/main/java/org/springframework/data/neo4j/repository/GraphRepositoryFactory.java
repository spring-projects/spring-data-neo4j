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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.repository.query.DerivedCypherRepositoryQuery;
import org.springframework.data.neo4j.support.GenericTypeExtractor;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.conversion.EntityResultConverter;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.neo4j.support.query.GremlinQueryEngine;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * @author mh
 * @since 28.03.11
 */
public class GraphRepositoryFactory extends RepositoryFactorySupport {

    private final GraphDatabaseContext graphDatabaseContext;
    private final MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext;

    /**
     * Creates a new {@link GraphRepositoryFactory} from the given {@link GraphDatabaseContext} and
     * {@link MappingContext}.
     * 
     * @param graphDatabaseContext must not be {@literal null}.
     * @param mappingContext must not be {@literal null}.
     */
    public GraphRepositoryFactory(GraphDatabaseContext graphDatabaseContext, MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {

        Assert.notNull(graphDatabaseContext);
        Assert.notNull(mappingContext);

        this.graphDatabaseContext = graphDatabaseContext;
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
        return getTargetRepository(metadata, graphDatabaseContext);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object getTargetRepository(RepositoryMetadata metadata, GraphDatabaseContext graphDatabaseContext) {

        Class<?> type = metadata.getDomainClass();
        GraphEntityInformation entityInformation = (GraphEntityInformation)getEntityInformation(type);
        // todo entityInformation.isGraphBacked();
        if (entityInformation.isNodeEntity()) {
            return new NodeGraphRepository(type,graphDatabaseContext);
        } else {
            return new RelationshipGraphRepository(type,graphDatabaseContext);
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
        return new GraphMetamodelEntityInformation(type,graphDatabaseContext);
    }



    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key) {
        return new QueryLookupStrategy() {
            @Override
            public RepositoryQuery resolveQuery(Method method, RepositoryMetadata repositoryMetadata, NamedQueries namedQueries) {
                final GraphQueryMethod queryMethod = new GraphQueryMethod(method, repositoryMetadata,namedQueries);

                if (!queryMethod.hasAnnotation() && !namedQueries.hasQuery(queryMethod.getNamedQueryName())) {
                    return new DerivedCypherRepositoryQuery(mappingContext, queryMethod, graphDatabaseContext);
                }

                return queryMethod.createQuery(repositoryMetadata, GraphRepositoryFactory.this.graphDatabaseContext);
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


        private Map<String, Object> resolveParams(Object[] parameters, GraphDatabaseContext graphDatabaseContext) {
            Map<String,Object> params=new HashMap<String, Object>();
            for (Parameter parameter : getParameters().getBindableParameters()) {
                final Object value = parameters[parameter.getIndex()];
                params.put(parameter.getName(),resolveParameter(value,graphDatabaseContext));
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

        private Object resolveParameter(Object parameter, GraphDatabaseContext graphDatabaseContext) {
            final Class<?> type = parameter.getClass();
            final PropertyContainer state = graphDatabaseContext.getPersistentState(parameter);
            if (graphDatabaseContext.isNodeEntity(type)) {
                return ((Node) state).getId();
            }
            if (graphDatabaseContext.isRelationshipEntity(type)) {
                return ((Relationship)state).getId();
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

        private RepositoryQuery createQuery(RepositoryMetadata repositoryMetadata, final GraphDatabaseContext context) {
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

        public CypherGraphRepositoryQuery(GraphQueryMethod queryMethod, RepositoryMetadata metadata, final GraphDatabaseContext graphDatabaseContext) {
            super(queryMethod, metadata, graphDatabaseContext);
            queryExecutor = new CypherQueryExecutor(graphDatabaseContext);
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
                    return queryExecutor.queryForList(queryString,params);
                }
                return queryExecutor.query(queryString, queryMethod.getCompoundType(),params);
            }
            return queryExecutor.queryForObject(queryString, queryMethod.getReturnType(),params);
        }
        private Object queryPaged(String queryString, Map<String, Object> params, Pageable pageable) {
            final Iterable<?> result = queryExecutor.query(queryString, getQueryMethod().getCompoundType(),params);
            return createPage(result, pageable);
        }
    }

    private static class GremlinGraphRepositoryQuery extends GraphRepositoryQuery {

        private GremlinQueryEngine queryExecutor;

        public GremlinGraphRepositoryQuery(GraphQueryMethod queryMethod, RepositoryMetadata metadata, final GraphDatabaseContext graphDatabaseContext) {
            super(queryMethod, metadata, graphDatabaseContext);
            queryExecutor = new GremlinQueryEngine(graphDatabaseContext.getGraphDatabaseService(), new EntityResultConverter<Object, Object>(graphDatabaseContext));
        }

        @Override
        protected Object dispatchQuery(String queryString, Map<String, Object> params, Pageable pageable) {
            GraphQueryMethod queryMethod = getQueryMethod();
            if (queryMethod.isPageQuery()) {
                return queryPaged(queryString,params,pageable);
            }
            if (queryMethod.isIterableResult()) {
                return queryExecutor.query(queryString,params).to(queryMethod.getCompoundType());
            }
            return queryExecutor.query(queryString, params).to(queryMethod.getReturnType()).single();
        }

        private Object queryPaged(String queryString, Map<String, Object> params, Pageable pageable) {
            final Iterable<?> result = queryExecutor.query(queryString, params).to(getQueryMethod().getCompoundType());
            return createPage(result, pageable);
        }

    }

    private static abstract class GraphRepositoryQuery implements RepositoryQuery {
        private final GraphQueryMethod queryMethod;
        private final GraphDatabaseContext graphDatabaseContext;

        public GraphRepositoryQuery(GraphQueryMethod queryMethod, RepositoryMetadata metadata, final GraphDatabaseContext graphDatabaseContext) {
            this.queryMethod = queryMethod;
            this.graphDatabaseContext = graphDatabaseContext;
        }

        @Override
        public Object execute(Object[] parameters) {
            Map<String, Object> params = queryMethod.resolveParams(parameters,graphDatabaseContext);
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