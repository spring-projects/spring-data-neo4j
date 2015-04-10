/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michal Bachman
 */
public class GraphModel  {

    private final Map<Long, NodeModel> nodeMap = new HashMap<>();

    private NodeModel[] nodes = new NodeModel[]{};
    private RelationshipModel[] relationships = new RelationshipModel[]{};

    public NodeModel[] getNodes() {
        return nodes;
    }

    public void setNodes(NodeModel[] nodes) {
        this.nodes = nodes;
        for (NodeModel node : nodes) {
            nodeMap.put(node.getId(), node);
        }
    }

    public RelationshipModel[] getRelationships() {
        return relationships;
    }

    public void setRelationships(RelationshipModel[] relationships) {
        this.relationships = relationships;
    }

    public NodeModel node(Long nodeId) {
        return nodeMap.get(nodeId);
    }

    /**
     * Determines whether or not this {@link GraphModel} contains a {@link NodeModel} that matches the specified ID.
     *
     * @param nodeId The graph node ID to match against a {@link NodeModel}
     * @return <code>true</code> if this {@link GraphModel} contains a node identified by the given argument, <code>false</code>
     *         if it doesn't
     */
    public boolean containsNodeWithId(Long nodeId) {
        return nodeMap.containsKey(nodeId);
    }

}
