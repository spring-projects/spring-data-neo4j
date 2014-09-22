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

import java.util.*;

import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;


public class RestEntityExtractor implements RestResultConverter {
    private final RestAPI restApi;

    public RestEntityExtractor(RestAPI restApi) {
        this.restApi = restApi;
    }

    public Object convertFromRepresentation(RequestResult requestResult) {
        return convertFromRepresentation(requestResult.toEntity());
    }

    @SuppressWarnings("unchecked")
    public Object convertFromRepresentation(Object value) {
        if (value instanceof Map) {
            if (canHandle(value)) {
                value = convertToEntityIfPossible(value);
            } else {
                final Map<String,Object> source = (Map<String,Object>) value;
                Map<String,Object> result=new HashMap<>(source.size());
                for (Map.Entry<String,Object> entry : source.entrySet()) {
                    result.put(entry.getKey(),convertFromRepresentation(entry.getValue()));
                }
                return result;
            }
        }
        if (value instanceof Iterable) {
            Collection<Object> result = value instanceof Set ? new HashSet<>() : new ArrayList<>();
            for (Object o : (Iterable) value) {
                result.add(convertFromRepresentation(o));
            }
            return result;
        }
        return value;
    }

    private Object convertToEntityIfPossible(Object value) {
        if (value instanceof Map) {
            RestEntity restEntity = restApi.createRestEntity((Map) value);
            if (restEntity != null) return restEntity;
        }
        return value;
    }

    public boolean canHandle(Object value) {
        if (value instanceof Map) {
            final String uri = (String) ((Map)value).get("self");
            if (uri != null && (uri.contains("/node/") || uri.contains("/relationship/"))){
                return true;
            }
        }
        return false;
    }
}
