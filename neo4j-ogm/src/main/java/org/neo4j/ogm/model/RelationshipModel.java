/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
