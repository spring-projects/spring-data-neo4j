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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author Michal Bachman
 */
public class RelationshipModel  {

    private Long id;
    private String type;
    private Long startNode;
    private Long endNode;
    List<Property<String, Object>> properties;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getStartNode() {
        return startNode;
    }

    public void setStartNode(Long startNode) {
        this.startNode = startNode;
    }

    public Long getEndNode() {
        return endNode;
    }

    public void setEndNode(Long endNode) {
        this.endNode = endNode;
    }

    public List<Property<String, Object>> getPropertyList() {
        return properties;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> map = new HashMap<>();
        for (Property<String, Object> property : properties) {
            map.put(property.getKey(), property.getValue());
        }
        return map;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = new ArrayList<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            this.properties.add(new Property<String, Object>(entry.getKey(), entry.getValue()));
        }
    }

    public void setPropertyList(List<Property<String, Object>> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RelationshipModel that = (RelationshipModel) o;

        if (!endNode.equals(that.endNode)) return false;
        if (!startNode.equals(that.startNode)) return false;
        if (!type.equals(that.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + startNode.hashCode();
        result = 31 * result + endNode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%d)-[%s]->(%d)", this.startNode, this.type, this.endNode);
    }

}
