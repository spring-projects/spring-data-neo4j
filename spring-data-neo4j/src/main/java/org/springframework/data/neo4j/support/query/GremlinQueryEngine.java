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

package org.springframework.data.neo4j.support.query;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jEdge;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jVertex;
import com.tinkerpop.gremlin.pipes.util.Table;

import org.neo4j.cypher.commands.Query;
import org.neo4j.cypher.javacompat.CypherParser;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.rest.repr.ListRepresentation;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.RepresentationType;
import org.neo4j.server.rest.repr.ValueRepresentation;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.QueryResult;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.ResultConverter;
import sun.security.provider.certpath.Vertex;

import javax.script.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GremlinQueryEngine implements QueryEngine {

    private final String g = "g";
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("gremlin");


    private ResultConverter resultConverter;
    private final DefaultQueryOperations queryOperations;
    private final GraphDatabaseService graphDatabaseService;

    public GremlinQueryEngine(GraphDatabaseService graphDatabaseService) {
        this(graphDatabaseService, new DefaultConverter());
    }


    public GremlinQueryEngine(GraphDatabaseService graphDatabaseService, ResultConverter resultConverter) {
        this.graphDatabaseService = graphDatabaseService;
        this.resultConverter = resultConverter != null ? resultConverter : new DefaultConverter();
        this.queryOperations = new DefaultQueryOperations(this);
    }


    @Override
    public QueryResult<Map<String, Object>> query(String statement) {
        try {
            final Neo4jGraph graph = new Neo4jGraph(graphDatabaseService);
            final Bindings bindings = new SimpleBindings();
            bindings.put(g, graph);
            final Object result = GremlinQueryEngine.engine.eval(statement, bindings);
            return getRepresentation(graph, result);
        } catch (final ScriptException e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }

        try {

            ExecutionResult result = parseAndExecuteQuery(statement);
            return new QueryResultBuilder<Map<String, Object>>(result, resultConverter);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }


    public static Representation getRepresentation(final Neo4jGraph graph,
                                                   final Object result) {
        if (result instanceof Iterable) {
            RepresentationType type = RepresentationType.STRING;
            final List<Representation> results = new ArrayList<Representation>();
            if (result instanceof Table) {
                type = RepresentationType.STRING;
                results.add(new GremlinTableRepresentation((Table) result, graph));
                return new ListRepresentation(type, results);
            }
            for (final Object r : (Iterable) result) {
                if (r instanceof Vertex) {
                    type = RepresentationType.NODE;
                    results.add(new NodeRepresentation(
                            ((Neo4jVertex) r).getRawVertex()));
                } else if (r instanceof Edge) {
                    type = RepresentationType.RELATIONSHIP;
                    results.add(new RelationshipRepresentation(
                            ((Neo4jEdge) r).getRawEdge()));
                } else if (r instanceof Graph) {
                    type = RepresentationType.STRING;
                    results.add(ValueRepresentation.string(graph.getRawGraph().toString()));
                } else if (r instanceof Double || r instanceof Float) {
                    type = RepresentationType.DOUBLE;
                    results.add(ValueRepresentation.number(((Number) r).doubleValue()));
                } else if (r instanceof Long || r instanceof Integer) {
                    type = RepresentationType.LONG;
                    results.add(ValueRepresentation.number(((Number) r).longValue()));
                } else {
                    System.out.println("GremlinPlugin: got back" + r);
                    type = RepresentationType.STRING;
                    results.add(ValueRepresentation.string(r.toString()));
                }
            }
            return new ListRepresentation(type, results);
        } else {
            return getSingleResult(graph, result);
        }
    }

    private static Object getSingleResult(Object result) {
        if (result instanceof Vertex) {
            return ((Neo4jVertex) result).getRawVertex();
        } else if (result instanceof Edge) {
            return ((Neo4jEdge) result).getRawEdge();
        } else if (result instanceof Graph) {
            return ValueRepresentation.string(graph.getRawGraph().toString());
        } else if (result instanceof Double || result instanceof Float) {
            return ValueRepresentation.number(((Number) result).doubleValue());
        } else if (result instanceof Long || result instanceof Integer) {
            return ValueRepresentation.number(((Number) result).longValue());
        } else {
            return ValueRepresentation.string(result + "");
        }
    }

    @Override
    public Iterable<Map<String, Object>> queryForList(String statement) {
        return queryOperations.queryForList(statement);
    }

    @Override
    public <T> Iterable<T> query(String statement, Class<T> type) {
        return queryOperations.query(statement, type);
    }

    @Override
    public <T> T queryForObject(String statement, Class<T> type) {
        return queryOperations.queryForObject(statement, type);
    }

    private ExecutionResult parseAndExecuteQuery(String statement) {
        try {
            CypherParser parser = new CypherParser();
            Query query = parser.parse(statement);
            return executionEngine.execute(query);
        } catch (Exception e) {
            throw new InvalidDataAccessResourceUsageException("Error executing statement " + statement, e);
        }
    }
}