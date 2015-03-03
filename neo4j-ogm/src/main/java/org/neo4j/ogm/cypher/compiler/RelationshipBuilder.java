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

package org.neo4j.ogm.cypher.compiler;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to compile Cypher that holds information about a relationship
 */
public abstract class RelationshipBuilder implements CypherEmitter, Comparable<RelationshipBuilder> {

    protected String type;
    protected String startNodeIdentifier;
    protected String endNodeIdentifier;
    protected String reference;

    private String direction;
    final Map<String, Object> props = new HashMap<>();


    protected RelationshipBuilder(String variableName) {
        this.reference = variableName;
    }

    public String getType() {
        return this.type;
    }

    public RelationshipBuilder type(String type) {
        this.type = type;
        return this;
    }

    public void addProperty(String propertyName, Object propertyValue) {
        this.props.put(propertyName, propertyValue);
    }

    public RelationshipBuilder direction(String direction) {
        this.direction = direction;
        return this;
    }

    public boolean hasDirection(String direction) {
        return this.direction != null && this.direction.equals(direction);
    }

    public abstract void relate(String startNodeIdentifier, String endNodeIdentifier);

    public String getReference() {
        return reference;
    }

    @Override
    public int compareTo(RelationshipBuilder o) {
        return reference.compareTo(o.reference);
    }

    @Override
    public String toString() {
        return "(" + startNodeIdentifier + ")-[" + reference + ":" + type + "]->(" + endNodeIdentifier + ")";
    }
}
