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

package org.neo4j.ogm.session.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.ogm.cypher.query.GraphModelQuery;
import org.neo4j.ogm.cypher.query.GraphRowModelQuery;
import org.neo4j.ogm.cypher.query.PagingAndSorting;
import org.neo4j.ogm.cypher.query.RowModelQuery;
import org.neo4j.ogm.cypher.query.RowModelQueryWithStatistics;
import org.neo4j.ogm.cypher.statement.ParameterisedStatement;
import org.neo4j.ogm.cypher.statement.ParameterisedStatements;
import org.neo4j.ogm.metadata.MappingException;
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.session.response.*;
import org.neo4j.ogm.session.result.GraphRowModel;
import org.neo4j.ogm.session.result.QueryStatistics;
import org.neo4j.ogm.session.result.RowModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class SessionRequestHandler implements RequestHandler {

    private final ObjectMapper mapper;
    private final Neo4jRequest<String> request;
    private final Logger logger = LoggerFactory.getLogger(SessionRequestHandler.class);

    public SessionRequestHandler(ObjectMapper mapper, Neo4jRequest<String> request) {
        this.request = request;
        this.mapper = mapper;
    }

    @Override
    public Neo4jResponse<GraphModel> execute(PagingAndSorting query, String url) {
        List<ParameterisedStatement> list = new ArrayList<>();
        list.add((GraphModelQuery) query);
        Neo4jResponse<String> response = execute(list, url);
        return new GraphModelResponse(response, mapper);
    }

    @Override
    public Neo4jResponse<RowModel> execute(RowModelQuery query, String url) {
        List<ParameterisedStatement> list = new ArrayList<>();
        list.add(query);
        Neo4jResponse<String> response = execute(list, url);
        return new RowModelResponse(response, mapper);
    }

    @Override
    public Neo4jResponse<GraphRowModel> execute(GraphRowModelQuery query, String url) {
        List<ParameterisedStatement> list = new ArrayList<>();
        list.add(query);
        Neo4jResponse<String> response = execute(list, url);
        return new GraphRowModelResponse(response, mapper);
    }

    @Override
    public Neo4jResponse<String> execute(ParameterisedStatement statement, String url) {
        List<ParameterisedStatement> list = new ArrayList<>();
        list.add(statement);
        return execute(list, url);
    }

    @Override
    public Neo4jResponse<QueryStatistics> execute(RowModelQueryWithStatistics query, String url) {
        List<ParameterisedStatement> list = new ArrayList<>();
        list.add(query);
        Neo4jResponse<String> response = execute(list, url);
        return new StatisticsResponse(response, mapper);
    }


    @Override
    public Neo4jResponse<String> execute(List<ParameterisedStatement> statementList, String url) {
        try {
            String json = mapper.writeValueAsString(new ParameterisedStatements(statementList));
            // ugh.
            if (!json.contains("statement\":\"\"")) {    // not an empty statement
                logger.debug(json);
                return request.execute(url, json);
            }
            return new EmptyResponse();
        } catch (JsonProcessingException jpe) {
            throw new MappingException(jpe.getLocalizedMessage());
        }
    }


}
