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
