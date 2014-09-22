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
package org.neo4j.rest.graphdb.traversal;

import org.junit.Test;
import org.neo4j.graphdb.Path;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RestPathParserTest {

    public static final String URI = "http://localhost:7470";

    @Test
    public void testParseZeroLength() throws Exception {
        String uri = "http://localhost:7470";
        RestAPI restApi = new RestAPIImpl(uri);
        Map<String,Object> node = MapUtil.map("data", Collections.EMPTY_MAP, "self", uri + "/db/data/node/0");
        Map<String, Object> pathData = MapUtil.map("start", node, "nodes",Collections.singletonList(node),"length", 0, "relationships", Collections.EMPTY_LIST, "end", node);
        System.out.println("pathData = " + pathData);
        Path path = RestPathParser.parse(pathData, restApi);
        assertEquals(0,path.length());
        assertEquals(null,path.lastRelationship());
        assertEquals(0,path.startNode().getId());
        assertEquals(0,path.endNode().getId());
    }
    @Test
    public void testParsePath() throws Exception {
        RestAPI restApi = new RestAPIImpl(URI);
        Map<String,Object> node = node(0);
        Map<String,Object> node2 = node(1);
        Map<String,Object> relationship = relationship(1,0,1);
        Map<String, Object> pathData = MapUtil.map("start", node, "nodes", Arrays.asList(node, node2),"length", 1, "relationships", Collections.singletonList(relationship), "end", node2);
        System.out.println("pathData = " + pathData);
        Path path = RestPathParser.parse(pathData, restApi);
        assertEquals(1,path.length());
        assertEquals(1,path.lastRelationship().getId());
        assertEquals(0,path.startNode().getId());
        assertEquals(1,path.endNode().getId());
    }

    private Map<String, Object> node(int id) {
        return MapUtil.map("data", Collections.EMPTY_MAP, "self", nodeUrl(id));
    }

    private String nodeUrl(int id) {
        return URI + "/db/data/node/" + id;
    }

    private String relationshipUrl(int id) {
        return URI + "/db/data/relationship/" + id;
    }

    private Map<String, Object> relationship(int id, int node1, int node2) {
        return MapUtil.map("data", Collections.EMPTY_MAP, "self", relationshipUrl(id),"start",nodeUrl(node1),"end",nodeUrl(node2));
    }
}
