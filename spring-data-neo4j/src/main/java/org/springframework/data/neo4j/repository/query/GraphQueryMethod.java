/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc.", "Neo Technology" / "Graph Aware Ltd."
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
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

import java.lang.reflect.Method;

/**
 * @author Mark Angrish
 */
public class GraphQueryMethod extends QueryMethod {

    private final Session session;
    private final Method method;
    private final Query queryAnnotation;

    public GraphQueryMethod(Method method, RepositoryMetadata metadata, Session session) {
        super(method, metadata);
        this.method = method;
        this.session = session;
        this.queryAnnotation = method.getAnnotation(Query.class);
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

    public RepositoryQuery createQuery() {
        return new GraphRepositoryQuery(this, session);
    }
}
