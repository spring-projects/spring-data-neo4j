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

import java.util.Map;

public class RestEntityExtractor {
    private final RestGraphDatabase restGraphDatabase;

    public RestEntityExtractor(RestGraphDatabase restGraphDatabase) {
        this.restGraphDatabase = restGraphDatabase;
    }

    Object convertFromRepresentation(Object value) {
        if (value instanceof Map) {
            RestEntity restEntity = createRestEntity((Map) value);
            if (restEntity != null) return restEntity;
        }
        return value;
    }

    RestEntity createRestEntity(Map data) {
        final String uri = (String) data.get("self");
        if (uri == null || uri.isEmpty()) return null;
        if (uri.contains("/node/")) {
            return new RestNode(data, restGraphDatabase);
        }
        if (uri.contains("/relationship/")) {
            return new RestRelationship(data, restGraphDatabase);
        }
        return null;
    }
}