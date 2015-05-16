/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.cypher.statement;

import org.neo4j.ogm.cypher.query.Orderings;
import org.neo4j.ogm.cypher.query.Paging;
import org.neo4j.ogm.cypher.query.PagingAndSorting;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple encapsulation of a Cypher query and its parameters and other optional parts (paging/sort).
 *
 * Note, this object will be transformed directly to JSON so don't add anything here that is
 * not part of the HTTP Transactional endpoint syntax
 *
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class ParameterisedStatement {

    private String statement;
    private Map<String, Object> parameters = new HashMap<>();
    private String[] resultDataContents;
    private boolean includeStats = false;

    private Paging paging;
    private Orderings orderings = new Orderings();


    /**
     * Constructs a new {@link ParameterisedStatement} based on the given Cypher query string and query parameters.
     *
     * @param cypher The parameterised Cypher query string
     * @param parameters The name-value pairs that satisfy the parameters in the given query
     */
    public ParameterisedStatement(String cypher, Map<String, ?> parameters) {
        this(cypher, parameters, "row");
    }

    protected ParameterisedStatement(String cypher, Map<String, ?> parameters, String... resultDataContents) {
        this.statement = cypher;
        this.parameters.putAll(parameters);
        this.resultDataContents = resultDataContents;
    }

    protected ParameterisedStatement(String cypher, Map<String, ?> parameters, boolean includeStats, String... resultDataContents) {
        this.statement = cypher;
        this.parameters.putAll(parameters);
        this.resultDataContents = resultDataContents;
        this.includeStats = includeStats;
    }

    public String getStatement() {

        StringBuilder sb = new StringBuilder();
        sb.append(statement.trim());

        sb.append(orderings);

        if (paging != null) {
            sb.append(paging);
        }

        return sb.toString().trim();
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String[] getResultDataContents() {
        return resultDataContents;
    }

    public boolean isIncludeStats() {
        return includeStats;
    }

    public Paging page() {
        return paging;
    }

    public Orderings orderings() {
        return orderings;
    }

    protected void addOrdering(PagingAndSorting.Direction direction, String... properties) {
        this.orderings.add(direction, properties);
    }

    protected void addPaging(Paging page) {
        this.paging = page;
    }
}

