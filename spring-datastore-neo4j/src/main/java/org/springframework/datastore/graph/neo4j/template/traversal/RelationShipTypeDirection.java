/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.datastore.graph.neo4j.template.traversal;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

import java.util.Arrays;
import java.util.List;

class RelationShipTypeDirection
{
    private final RelationshipType relationshipType;
    private final Direction direction;

    private RelationShipTypeDirection(final RelationshipType relationshipType, final Direction direction)
    {
        if (relationshipType == null)
            throw new IllegalArgumentException("RelationShipType must not be null");
        if (direction == null)
            throw new IllegalArgumentException("Direction must not be null");
        this.relationshipType = relationshipType;
        this.direction = direction;
    }

    public static RelationShipTypeDirection incoming(RelationshipType relationshipType)
    {
        return new RelationShipTypeDirection(relationshipType, Direction.INCOMING);
    }

    public static RelationShipTypeDirection outgoing(RelationshipType relationshipType)
    {
        return new RelationShipTypeDirection(relationshipType, Direction.OUTGOING);
    }

    public static RelationShipTypeDirection twoway(RelationshipType relationshipType)
    {
        return new RelationShipTypeDirection(relationshipType, Direction.BOTH);
    }

    public RelationshipType getRelationshipType()
    {
        return relationshipType;
    }

    public Direction getDirection()
    {
        return direction;
    }

    public List<Object> asList()
    {
        return Arrays.asList(relationshipType, direction);
    }
}
