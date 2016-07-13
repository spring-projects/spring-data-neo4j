/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
import java.util.Map;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.annotations.EntityFactory;
import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.request.Request;
import org.neo4j.ogm.session.GraphCallback;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.transaction.Transaction;
import org.springframework.data.repository.query.ParameterAccessor;

/**
 * Specialisation of {@link GraphRepositoryQuery} that handles mapping to objects annotated with <code>&#064;QueryResult</code>.
 *
 * @author Adam George
 * @author Luanne Misquitta
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
    protected Object execute(Class<?> returnType, final Class<?> concreteReturnType, String cypherQuery, Map<String, Object> queryParams, ParameterAccessor parameterAccessor) {
        Collection<Object> resultObjects = concreteReturnType.isInterface()
                ? mapToProxy(concreteReturnType, cypherQuery, queryParams)
                : mapToConcreteType(concreteReturnType, cypherQuery, queryParams);

        if (Iterable.class.isAssignableFrom(returnType)) {
            return resultObjects;
        }
        return resultObjects.isEmpty() ? null : resultObjects.iterator().next();
    }

    private Collection<Object> mapToConcreteType(final Class<?> targetType, final String cypherQuery, final Map<String, Object> queryParams) {



        return this.session.doInTransaction(new GraphCallback<Collection<Object>>() {
            @Override
            public Collection<Object> apply(Request requestHandler, Transaction transaction, MetaData metaData) {
                Collection<Object> toReturn = new ArrayList<>();
                SingleUseEntityMapper entityMapper = new SingleUseEntityMapper(metaData, new EntityFactory(metaData));
                Iterable<Map<String,Object>> results = session.query(cypherQuery, queryParams);
                for (Map<String,Object> result : results) {
                  toReturn.add(entityMapper.map(targetType, result));
                }
                return toReturn;
            }
        });
    }

    private Collection<Object> mapToProxy(Class<?> targetType, String cypherQuery, Map<String, Object> queryParams) {
        Iterable<Map<String, Object>> queryResults = this.session.query(cypherQuery, queryParams);

        Collection<Object> resultObjects = new ArrayList<>();
        Class<?>[] interfaces = new Class<?>[] {targetType};
        for (Map<String, Object> map : queryResults) {
            resultObjects.add(newProxyInstance(targetType.getClassLoader(), interfaces, new QueryResultProxy(map)));
        }
        return resultObjects;
    }

}
