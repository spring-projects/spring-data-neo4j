/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.repository.query;

import org.neo4j.ogm.session.result.QueryStatistics;
import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.query.derived.DerivedGraphRepositoryQuery;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 */
public class GraphQueryMethod extends QueryMethod {

    private final Session session;
    private final Method method;
    private final Query queryAnnotation;
    private final RepositoryMetadata metadata;
    private final Neo4jMappingContext mappingContext;

    public GraphQueryMethod(Method method, RepositoryMetadata metadata, Session session, Neo4jMappingContext mappingContext) {
        super(method, metadata);
        this.method = method;
        this.session = session;
        this.queryAnnotation = method.getAnnotation(Query.class);
        this.metadata = metadata;
        this.mappingContext = mappingContext;
    }

    public String getQuery() {
        return queryAnnotation.value();
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public String getNamedQueryName() {
        throw new UnsupportedOperationException("OGM does not currently support named queries.");
    }

    /**
     * @return The concrete, non-generic return type of this query method - i.e., the type to which graph database query results
     *         should be mapped
     */
    public Class<?> resolveConcreteReturnType() {
        Class<?> type = this.method.getReturnType();
        Type genericType = this.method.getGenericReturnType();

        if (Iterable.class.isAssignableFrom(type)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType returnType = (ParameterizedType) genericType;
                Type componentType = returnType.getActualTypeArguments()[0];

                return componentType instanceof ParameterizedType ?
                        (Class<?>) ((ParameterizedType) componentType).getRawType() :
                        (Class<?>) componentType;
            } else {
                return Object.class;
            }
        }

        return type;
    }

    public RepositoryQuery createQuery() {
        if (method.getAnnotation(Query.class) != null) {
            if (resolveConcreteReturnType().isAnnotationPresent(QueryResult.class)) {
                return new QueryResultGraphRepositoryQuery(this, session);
            }
            return new GraphRepositoryQuery(this, session);
        }
        return new DerivedGraphRepositoryQuery(this, session, mappingContext);

    }

    @Override
    public boolean isModifyingQuery() {
        return method.getReturnType().isAssignableFrom(QueryStatistics.class);
    }
}
