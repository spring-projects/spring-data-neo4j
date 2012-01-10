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
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jEdge;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jVertex;
import com.tinkerpop.pipes.util.Table;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.IterableWrapper;

import javax.script.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GremlinExecutor {

    public static final int REFRESH_ENGINE_COUNT = 10000;
    private static final String GRAPH_VARIABLE = "g";
    private volatile ScriptEngine engine;

    private ScriptEngine createScriptEngine() {
        return new ScriptEngineManager().getEngineByName("gremlin-groovy");
    }

    private static final AtomicInteger executionCount = new AtomicInteger();
    private final GraphDatabaseService graphDatabaseService;

    public GremlinExecutor(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    @SuppressWarnings("unchecked")
    public Iterable<Object> query(String statement, Map<String,Object> params) {
        try {
            final Bindings bindings = createBindings(params);
            final ScriptEngine engine = engine();
            final Object result = engine.eval(statement, bindings);
            return getRepresentation(result);
        } catch (final ScriptException e) {
            throw new RuntimeException("Error executing statement " + statement, e);
        }
    }

    private Bindings createBindings(Map<String, Object> params) {
        final Bindings bindings = new SimpleBindings();
        bindings.put(GRAPH_VARIABLE, new Neo4jGraph(graphDatabaseService,false));
        if (params==null) return bindings;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            bindings.put(entry.getKey(),entry.getValue());
        }
        return bindings;
    }

    private ScriptEngine engine() {
        if (engine == null || executionCount.incrementAndGet() > REFRESH_ENGINE_COUNT) {
            executionCount.set(0);
            this.engine = createScriptEngine();
        }
        return this.engine;
    }


    @SuppressWarnings("unchecked")
    public static Iterable getRepresentation(final Object result) {
        if (result instanceof Iterable) {
            if (result instanceof Table) {
                final Table table = (Table) result;
                return new IterableWrapper<Map<String,Object>,Table.Row>(table) {
                    @Override
                    protected Map<String, Object> underlyingObjectToObject(Table.Row row) {
                        Map<String,Object> result=new LinkedHashMap<String, Object>();
                        for (String column : table.getColumnNames()) {
                            result.put(column, row.getColumn(column));
                        }
                        return result;
                    }
                };
            }
            return new IterableWrapper((Iterable) result) {
                @Override
                protected Object underlyingObjectToObject(Object object) {
                    return getSingleResult(object);
                }
            };
        } else {
            return Collections.singleton(getSingleResult(result));
        }
    }

    private static Object getSingleResult(Object result) {
        if (result instanceof Vertex) {
            return ((Neo4jVertex) result).getRawVertex();
        } else if (result instanceof Edge) {
            return ((Neo4jEdge) result).getRawEdge();
        } else if (result instanceof Neo4jGraph) {
            return ((Neo4jGraph) result).getRawGraph();
        } else {
            return result;
        }
    }
}