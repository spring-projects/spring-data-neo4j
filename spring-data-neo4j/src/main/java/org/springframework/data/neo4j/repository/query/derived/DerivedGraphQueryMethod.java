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

package org.springframework.data.neo4j.repository.query.derived;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.query.GraphQueryMethod;
import org.springframework.data.repository.core.RepositoryMetadata;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by markangrish on 20/01/2015.
 */
public class DerivedGraphQueryMethod extends GraphQueryMethod {

    private static final Pattern PREFIX_TEMPLATE = Pattern.compile("^(find|read|get|query)((\\p{Lu}.*?))??By");


    private String query;

    public DerivedGraphQueryMethod(Method method, RepositoryMetadata metadata, Session session) {
        super(method, metadata, session);
        //TODO: should this be eager?
        this.query = buildQuery(method, metadata.getDomainType());
    }

    // FIXME: The hackiest thing i could do to get something working.
    // The easiest way to get this working is to use the existing graph repository dispatching and just override the
    // getQuery() method to return a derived string.
    private String buildQuery(Method method, Class<?> type) {
        String methodName = method.getName();
        Matcher matcher = PREFIX_TEMPLATE.matcher(methodName);
        if (!matcher.find()) {
            throw new RuntimeException("Could not derive query for method: " + methodName + ". Check spelling or use @Query.");
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("MATCH (o:");

        //TODO: check NodeEntity annotation for label/s. Work out how to support RelationshipEntity.
        queryBuilder.append(type.getSimpleName());
        queryBuilder.append(") WHERE ");

        // TODO: This will be broken down with another regex.
        // This will be a for each through each token in the predicates string; i = 0 to match query parameter number.
        String predicates = methodName.substring(matcher.group().length());
        queryBuilder.append("o.");

        // FIXME: Shady way up lowercasing the camel casing to match a propertyName!
        queryBuilder.append(predicates.substring(0, 1).toLowerCase() + predicates.substring(1));

        //TODO: Use a lookup table to match SDC behaviour to actual sign (see Part.java)
        queryBuilder.append(" = ");
        queryBuilder.append("{0}");

        queryBuilder.append(" RETURN o");
        return queryBuilder.toString();
    }

    public String getQuery() {
        return query;
    }
}
