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

package org.springframework.data.neo4j.rest.support;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 02.03.11
 */
public class Neo4jDatabaseCleaner {
    private GraphDatabaseService graph;

    public Neo4jDatabaseCleaner(GraphDatabaseService graph) {
        this.graph = graph;
    }

    public Map<String, Object> cleanDb() {
        Map<String, Object> result = new HashMap<String, Object>();
        try (Transaction tx = graph.beginTx()) {
            removeNodes(result);
            clearIndex(result);
            tx.success();
        }
        return result;
    }

    private void removeNodes(Map<String, Object> result) {
        int nodes = 0, relationships = 0;
        for (Node node : graph.getAllNodes()) {
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                rel.delete();
                relationships++;
            }
            node.delete();
            nodes++;
        }
        result.put("nodes", nodes);
        result.put("relationships", relationships);

    }

    private void clearIndex(Map<String, Object> result) {
        IndexManager indexManager = graph.index();
        result.put("node-indexes", Arrays.asList(indexManager.nodeIndexNames()));
        result.put("relationship-indexes", Arrays.asList(indexManager.relationshipIndexNames()));
        for (String ix : indexManager.nodeIndexNames()) {
            indexManager.forNodes(ix).delete();
        }
        for (String ix : indexManager.relationshipIndexNames()) {
            indexManager.forRelationships(ix).delete();
        }
    }
}
