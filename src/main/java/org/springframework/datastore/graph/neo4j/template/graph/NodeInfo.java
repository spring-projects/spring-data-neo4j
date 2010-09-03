package org.springframework.datastore.graph.neo4j.template.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.datastore.graph.neo4j.template.Graph;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class NodeInfo
{

    private final String name;
    private final Map<String, Object> props = new LinkedHashMap<String, Object>();

    private final Set<RelationShipNodeInfo> relations = new LinkedHashSet<RelationShipNodeInfo>();
    private long id;

    public NodeInfo(final String nodeName)
    {
        this.name = nodeName;
    }

    public NodeInfo setProperty(final String name, final Object value)
    {
        this.props.put(name, value);
        return this;
    }

    public void relateTo(final RelationshipType type, final NodeInfo node)
    {
        this.relations.add(new RelationShipNodeInfo(type, node));
    }

    public void setId(final long id)
    {
        this.id = id;
    }

    public long getId()
    {
        return id;
    }

    void updateProperties(final Node node)
    {
        node.setProperty("name", name);
        for (Map.Entry<String, Object> prop : props.entrySet())
        {
            node.setProperty(prop.getKey(), prop.getValue());
        }
        setId(node.getId());
    }

    void updateRelations(final Node from, final Graph nodeService)
    {
        for (RelationShipNodeInfo relation : relations)
        {
            final Node to = nodeService.getNodeById(relation.getNodeInfo().getId());
            from.createRelationshipTo(to, relation.getRelationshipType());
        }
    }

    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final NodeInfo nodeInfo = (NodeInfo) o;

        return name.equals(nodeInfo.name);

    }

    public int hashCode()
    {
        return name.hashCode();
    }
}
