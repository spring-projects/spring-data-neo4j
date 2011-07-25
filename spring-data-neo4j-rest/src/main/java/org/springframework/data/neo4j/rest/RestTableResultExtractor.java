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

package org.springframework.data.neo4j.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 25.07.11
 */
public class RestTableResultExtractor {

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
}
