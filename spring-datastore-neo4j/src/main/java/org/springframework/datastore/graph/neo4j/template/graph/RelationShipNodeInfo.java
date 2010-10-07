package org.springframework.datastore.graph.neo4j.template.graph;

import org.neo4j.graphdb.RelationshipType;

class RelationShipNodeInfo
{
    private final RelationshipType relationshipType;
    private final NodeInfo nodeInfo;

    RelationShipNodeInfo(final RelationshipType relationshipType, final NodeInfo nodeInfo)
    {
        this.relationshipType = relationshipType;
        this.nodeInfo = nodeInfo;
    }

    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RelationShipNodeInfo that = (RelationShipNodeInfo) o;

        return nodeInfo.equals(that.nodeInfo) && relationshipType.name().equals(that.relationshipType.name());

    }

    public int hashCode()
    {
        return relationshipType.name().hashCode() * 31 + nodeInfo.hashCode();
    }

    public NodeInfo getNodeInfo()
    {
        return nodeInfo;
    }

    public RelationshipType getRelationshipType()
    {
        return relationshipType;
    }
}
