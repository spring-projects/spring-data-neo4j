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
package org.neo4j.rest.graphdb;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.rest.graphdb.index.RestAutoIndexer;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 * @author mh
 * @since 09.05.11
 */
public class Neo4jDatabaseCleaner {
    private GraphDatabaseService graph;

    public Neo4jDatabaseCleaner(GraphDatabaseService graph) {
        this.graph = graph;
    }

    public Map<String, Object> cleanDb() {
//        if (graph instanceof ImpermanentGraphDatabase) {
//            ((ImpermanentGraphDatabase)graph).cleanContent();
//            return Collections.emptyMap();
//        }
        Map<String, Object> result = new HashMap<String, Object>();
        Transaction tx = graph.beginTx();
        try {
            removeNodes(result);
            clearIndex(result);
            tx.success();
        } finally {
            tx.close();
        }
        return result;
    }

    private void removeNodes(Map<String, Object> result) {
        int nodes = 0, relationships = 0;
        for (Node node : GlobalGraphOperations.at(graph).getAllNodes()) {
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
            deleteIndex(indexManager.forNodes(ix));
        }
        for (String ix : indexManager.relationshipIndexNames()) {
            deleteIndex(indexManager.forRelationships(ix));
        }
    }

    private void deleteIndex(Index index) {
        try {
            index.delete();
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }
}
