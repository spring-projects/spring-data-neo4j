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
package org.neo4j.rest.graphdb.index;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.graphdb.index.ReadableRelationshipIndex;
import org.neo4j.graphdb.index.RelationshipAutoIndexer;
import org.neo4j.rest.graphdb.RestAPI;

public class RestRelationshipAutoIndexer extends RestAutoIndexer<Relationship> implements RelationshipAutoIndexer {

    public RestRelationshipAutoIndexer(RestAPI restApi, ReadableRelationshipIndex autoIndex) {
        super(restApi, Relationship.class, autoIndex);
    }

    public ReadableRelationshipIndex getAutoIndex() {
        return (ReadableRelationshipIndex)super.getAutoIndex();
    }
}
