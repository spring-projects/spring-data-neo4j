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

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.query.derived.DerivedGraphQueryMethod;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import java.lang.reflect.Method;

/**
 * @author Mark Angrish
 */
public class GraphQueryLookupStrategy implements QueryLookupStrategy {

    private final Session session;

    public GraphQueryLookupStrategy(Session session, QueryLookupStrategy.Key key,
            EvaluationContextProvider evaluationContextProvider) {
        this.session = session;
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata repositoryMetadata, NamedQueries namedQueries) {

        if (method.getAnnotation(Query.class) != null) {
            return new GraphQueryMethod(method, repositoryMetadata, session).createQuery();
        }

        return new DerivedGraphQueryMethod(method, repositoryMetadata, session).createQuery();
    }
}
