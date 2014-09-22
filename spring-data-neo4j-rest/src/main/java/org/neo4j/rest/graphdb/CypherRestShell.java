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

import org.neo4j.rest.graphdb.query.CypherRestResult;
import org.neo4j.rest.graphdb.query.CypherResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CypherRestShell {
    public static void main(String[] args) throws IOException {
        String uri = (args.length>0) ? args[0] : "http://localhost:7474/db/data";
        RestAPIImpl restAPIFacade = args.length>1 ? new RestAPIImpl(uri,args[1],args[2]) : new RestAPIImpl(uri);
        System.out.println("Connected to "+uri);
        try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String query;
        System.out.print("Query: ");
        while ((query=reader.readLine())!=null && !query.isEmpty()) {
            long time=System.currentTimeMillis();
            CypherResult result = restAPIFacade.query(query, null);
            time=System.currentTimeMillis()-time;
            System.out.println(result.getColumns());
            List<List<Object>> rows = (List<List<Object>>) result.getData();
            for (List<Object> row : rows) {
                System.out.println(row);
            }
            System.out.println(rows.size()+" row(s), roundtrip time "+time+" ms.");
            System.out.print("Query: ");
        }
        } finally {
            restAPIFacade.close();
        }
    }
}
