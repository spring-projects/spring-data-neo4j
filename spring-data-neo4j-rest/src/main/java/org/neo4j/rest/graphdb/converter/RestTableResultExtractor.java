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
package org.neo4j.rest.graphdb.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.rest.graphdb.util.JsonHelper;

public class RestTableResultExtractor implements RestResultConverter{

    private final RestEntityExtractor restEntityExtractor;

    public RestTableResultExtractor(RestEntityExtractor restEntityExtractor) {
        this.restEntityExtractor = restEntityExtractor;
    }


    public List<Map<String, Object>> extract(Map<?, ?> restResult) {
        List<String> columns = (List<String>) restResult.get("columns");
        return extractData(restResult, columns);
    }

    private List<Map<String, Object>> extractData(Map<?, ?> restResult, List<String> columns) {
        List<List<?>> rows = (List<List<?>>) restResult.get("data");
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(rows.size());
        for (List<?> row : rows) {
            result.add(mapRow(columns, row));
        }
        return result;
    }

    private Map<String, Object> mapRow(List<String> columns, List<?> row) {
        int columnCount = columns.size();
        Map<String, Object> newRow = new HashMap<String, Object>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            final Object value = row.get(i);
            newRow.put(columns.get(i), restEntityExtractor.convertFromRepresentation(value));
        }
        return newRow;
    }

    @Override
    public Object convertFromRepresentation(RequestResult value) {
        return extract(toMap(value));
    }

    public boolean canHandle(Object restResult){
        return restResult instanceof Map && ((Map)restResult).containsKey("columns") && ((Map)restResult).containsKey("data");
    }

    public Map<?, ?> toMap(RequestResult requestResult) {
	    return requestResult.toMap();
	}
}