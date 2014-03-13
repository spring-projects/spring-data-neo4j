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
package org.springframework.data.neo4j.support;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 04.01.14
 */
public class ReferenceNodes {

    public static final String ROOT_NAME = "root";
    private static ExecutionEngine engine;
    private static GraphDatabaseService dbRef;

    public static Node getReferenceNode(GraphDatabaseService db) {
        return getReferenceNode(db, ROOT_NAME);
    }
    public static Node getReferenceNode(GraphDatabase db) {
        return getReferenceNode(db, ROOT_NAME);
    }
    public static Node obtainReferenceNode(GraphDatabaseService db) {
        return obtainReferenceNode(db, ROOT_NAME);
    }
    public static Node obtainReferenceNode(GraphDatabase db) {
        return obtainReferenceNode(db, ROOT_NAME);
    }

    public static Node obtainReferenceNode(GraphDatabaseService db, String name) {
        return executeQuery(db, name, "MERGE (ref:ReferenceNode {name:{name}}) RETURN ref");
    }

    private static Node executeQuery(GraphDatabaseService db, String name, String query) {
        if (engine == null || db != dbRef) {
            engine = new ExecutionEngine(db);
            dbRef = db;
        }

        ExecutionResult result = engine.execute(query, map("name", name));
        return IteratorUtil.single(result.<Node>columnAs("ref"));
    }

    public static Node getReferenceNode(GraphDatabaseService db, String name) {
        return executeQuery(db, name, "MATCH (ref:ReferenceNode {name:{name}}) RETURN ref");
    }

    public static Node obtainReferenceNode(GraphDatabase db, String name) {
        return executeQuery(db, name, "MERGE (ref:ReferenceNode {name:{name}}) RETURN ref");
    }

    private static Node executeQuery(GraphDatabase db, String name, String query) {
        CypherQueryEngine engine = db.queryEngine();
        return engine.query(query,map("name", name)).to(Node.class).singleOrNull();
    }

    public static Node getReferenceNode(GraphDatabase db, String name) {
        return executeQuery(db, name, "MATCH (ref:ReferenceNode {name:{name}}) RETURN ref");
    }
}
