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

package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

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
