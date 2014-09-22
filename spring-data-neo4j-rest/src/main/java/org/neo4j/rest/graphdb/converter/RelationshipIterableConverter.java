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

import java.util.Collection;
import java.util.Map;

import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IterableWrapper;

import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.entity.RestRelationship;

/**
* @author mh
* @since 22.09.11
*/
public class RelationshipIterableConverter implements RestResultConverter {
    private final RestAPI restAPI;

    public RelationshipIterableConverter(RestAPI restAPI) {
        this.restAPI = restAPI;
    }

    @Override
    public Object convertFromRepresentation(RequestResult requestResult) {
        return new IterableWrapper<Relationship, Object>((Collection<Object>) requestResult.toEntity()) {
            @Override
            protected Relationship underlyingObjectToObject(Object data) {
                return new RestRelationship((Map<?, ?>) data, restAPI);
            }
        };
    }
}
