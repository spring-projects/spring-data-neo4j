/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb.query;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.rest.graphdb.*;
import org.neo4j.rest.graphdb.converter.RestEntityExtractor;
import org.neo4j.rest.graphdb.converter.RestTableResultExtractor;
import org.neo4j.rest.graphdb.util.ConvertedResult;
import org.neo4j.rest.graphdb.util.DefaultConverter;
import org.neo4j.rest.graphdb.util.Handler;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.neo4j.rest.graphdb.util.QueryResultBuilder;
import org.neo4j.rest.graphdb.util.ResultConverter;

public class RestCypherQueryEngine implements QueryEngine<Map<String,Object>> {
    private final RestAPI restApi;
    private final ResultConverter resultConverter;

    public RestCypherQueryEngine(RestAPI restApi) {
        this(restApi,null);
    }
    public RestCypherQueryEngine(RestAPI restApi, ResultConverter resultConverter) {
        this.restApi = restApi;
        this.resultConverter = resultConverter!=null ? resultConverter : new DefaultConverter();
    }
    
    @Override
    public QueryResult<Map<String, Object>> query(String statement, Map<String, Object> params) {
        return restApi.query(statement, params, this.resultConverter);
    }
}