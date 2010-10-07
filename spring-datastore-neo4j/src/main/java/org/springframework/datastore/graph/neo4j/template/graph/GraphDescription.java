package org.springframework.datastore.graph.neo4j.template.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.datastore.graph.neo4j.template.Graph;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class GraphDescription
{

    private Map<String, NodeInfo> nodes = new LinkedHashMap<String, NodeInfo>();

    public GraphDescription(final Properties props)
    {
        if (props == null)
            throw new IllegalArgumentException("Properties must not be null");
        new PropertyParser(props).load(this);
    }


    public GraphDescription()
    {
    }

    public NodeInfo add(final String nodeName, final String attributeName, final Object value)
    {
        return getNode(nodeName).setProperty(attributeName, value);
    }

    private NodeInfo getNode(final String nodeName)
    {
        final NodeInfo node = nodes.get(nodeName);
        if (node != null) return node;
        final NodeInfo newNode = new NodeInfo(nodeName);
        nodes.put(nodeName, newNode);
        return newNode;
    }

    public void relate(final String from, final RelationshipType type, final String to)
    {
        getNode(from).relateTo(type, getNode(to));
    }

    void addToGraph(Graph graph)
    {
        boolean first = true;
        for (NodeInfo nodeInfo : nodes.values())
        {
            final Node node = first ? graph.getReferenceNode() : graph.createNode();
            first = false;
            nodeInfo.updateProperties(node);
        }

        for (NodeInfo nodeInfo : nodes.values())
        {
            final Node from = graph.getNodeById(nodeInfo.getId());
            nodeInfo.updateRelations(from, graph);
        }
    }

}
