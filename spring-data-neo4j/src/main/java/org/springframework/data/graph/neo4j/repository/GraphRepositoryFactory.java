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

package org.springframework.data.graph.neo4j.repository;

import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.graph.annotation.Query;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.GenericTypeExtractor;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.query.QueryExecutor;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.*;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author mh
 * @since 28.03.11
 */
public class GraphRepositoryFactory extends RepositoryFactorySupport {


    private final GraphDatabaseContext graphDatabaseContext;

    public GraphRepositoryFactory(GraphDatabaseContext graphDatabaseContext) {
        Assert.notNull(graphDatabaseContext);
        this.graphDatabaseContext = graphDatabaseContext;
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
        Class<?> repositoryInterface = metadata.getRepositoryInterface();
        Class<?> type = metadata.getDomainClass();
        GraphEntityInformation entityInformation = (GraphEntityInformation)getEntityInformation(type);

        if (entityInformation.isNodeEntity()) {
            return new NodeGraphRepository(type,graphDatabaseContext);
        } else {
            return new RelationshipGraphRepository(type,graphDatabaseContext);
        }
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        Class<?> domainClass = repositoryMetadata.getDomainClass();
        if (findAnnotation(domainClass, NodeEntity.class) !=null) {
            return NodeGraphRepository.class;
        }
        if (findAnnotation(domainClass, RelationshipEntity.class) !=null) {
            return RelationshipGraphRepository.class;
        }
        throw new IllegalArgumentException("Invalid Domain Class "+ domainClass+" neither Node- nor RelationshipEntity");
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> type) {
        return new GraphMetamodelEntityInformation(type,graphDatabaseContext);
    }



    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key) {
        return new QueryLookupStrategy() {
            @Override
            public RepositoryQuery resolveQuery(Method method, RepositoryMetadata repositoryMetadata, NamedQueries namedQueries) {
                final GraphQueryMethod queryMethod = new GraphQueryMethod(method, repositoryMetadata,namedQueries);

                if (queryMethod.isValid()) {
                    return new GraphRepositoryQuery(queryMethod, repositoryMetadata, graphDatabaseContext);
                }
                return null;
            }
        };
    }

    static class GraphQueryMethod extends QueryMethod {

        private final Method method;
        private final Query queryAnnotation;
        private final String query;

        public GraphQueryMethod(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {
            super(method, metadata);
            this.method = method;
            queryAnnotation = method.getAnnotation(Query.class);
            this.query = queryAnnotation != null ? queryAnnotation.value() : getNamedQuery(namedQueries);
            if (this.query==null) throw new IllegalArgumentException("Could not extract a query from "+method);
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
            Object[] resolvedParameters = resolveParameters(args,parameters.getBindableParameters());
            String baseQuery = String.format(query, (Object[]) resolvedParameters);
            if (parameters.hasSortParameter()) {
                baseQuery = addSorting(baseQuery, (Sort) args[parameters.getSortIndex()]);
            }
            if (parameters.hasPageableParameter()) {
                final Pageable pageable = getPageable(args);
                if (pageable!=null) {
                    baseQuery = addSorting(baseQuery, pageable.getSort());
                    baseQuery = addPaging(baseQuery, pageable);
                }
            }
            return baseQuery;
        }

        private Pageable getPageable(Object[] args) {
            Parameters parameters = getParameters();
            if (parameters.hasPageableParameter()) return (Pageable) args[parameters.getPageableIndex()];
            return null;
        }

        private String addPaging(String baseQuery, Pageable pageable) {
            if (pageable==null) return baseQuery;
            return baseQuery + " skip "+pageable.getOffset() + " limit " + pageable.getPageSize();
        }

        private String addSorting(String baseQuery, Sort sort) {
            if (sort==null) return baseQuery; // || sort.isEmpty()
            final String sortOrder = getSortOrder(sort);
            if (sortOrder.isEmpty()) return baseQuery;
            return baseQuery + " order by " + sortOrder;
        }

        private String getSortOrder(Sort sort) {
            String result = "";
            for (Sort.Order order : sort) {
                result += order.getProperty() + " " + order.getDirection();
            }
            return result;
        }

        private Object[] resolveParameters(Object[] parameters, Parameters bindableParameters) {
            final int paramCount = bindableParameters.getNumberOfParameters();
            final Object[] result = new Object[paramCount];
            for (int i = 0; i < paramCount; i++) {
                final Parameter parameter = bindableParameters.getParameter(i);
                final Object value = parameters[parameter.getIndex()];
                result[i] = resolveParameter(value);
            }
            return result;
        }

        private Object resolveParameter(Object parameter) {
            if (parameter instanceof NodeBacked) {
                return ((NodeBacked)parameter).getNodeId();
            }
            if (parameter instanceof RelationshipBacked) {
                return ((RelationshipBacked)parameter).getRelationshipId();
            }
            return parameter;
        }

        private Class<?> getCompoundType() {
            final Class<?> elementClass = getElementClass();
            if (elementClass!=null) return elementClass;
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
    }

    private static class GraphRepositoryQuery implements RepositoryQuery {
        private QueryExecutor queryExecutor;
        private final GraphQueryMethod queryMethod;
        private final RepositoryMetadata metadata;
        private boolean iterableResult;
        private Class<?> compoundType;

        public GraphRepositoryQuery(GraphQueryMethod queryMethod, RepositoryMetadata metadata, final GraphDatabaseContext graphDatabaseContext) {
            queryExecutor = new QueryExecutor(graphDatabaseContext);
            this.queryMethod = queryMethod;
            this.metadata = metadata;
            this.iterableResult = Iterable.class.isAssignableFrom(queryMethod.getReturnType());
            this.compoundType = queryMethod.getCompoundType();
        }

        @Override
        public Object execute(Object[] parameters) {
            final String queryString = queryMethod.prepareQuery(parameters);
            return dispatchQuery(queryString,queryMethod.getPageable(parameters));
        }

        private Object dispatchQuery(String queryString, Pageable pageable) {
            final QueryMethod.Type queryResultType = queryMethod.getType();
            if (queryResultType== QueryMethod.Type.PAGING) {
                return queryPaged(queryString,pageable);
            }
            if (iterableResult) {
                if (compoundType.isAssignableFrom(Map.class)) return queryExecutor.queryForList(queryString);
                return queryExecutor.query(queryString, queryMethod.getCompoundType());
            }
            return queryExecutor.queryForObject(queryString, queryMethod.getReturnType());
        }

        private Object queryPaged(String queryString, Pageable pageable) {
            final Iterable<?> result = queryExecutor.query(queryString, queryMethod.getCompoundType());
            return createPage(result, pageable);
        }

        @SuppressWarnings({"unchecked"})
        private Object createPage(Iterable<?> result, Pageable pageable) {
            final List resultList = IteratorUtil.addToCollection(result, new ArrayList());
            if (pageable==null) return new PageImpl(resultList);
            final int currentTotal = pageable.getOffset() + pageable.getPageSize();
            return new PageImpl(resultList, pageable, currentTotal);
        }

        @Override
        public GraphQueryMethod getQueryMethod() {
            return queryMethod;
        }
    }
}