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

import org.neo4j.ogm.entityaccess.EntityAccessStrategy;
import org.neo4j.ogm.metadata.info.ClassInfo;

import java.util.*;

public abstract class NodeBuilder implements CypherEmitter, Comparable<NodeBuilder> {

    private final String cypherReference;

    final Map<String, Object> props = new HashMap<>();
    final List<String> labels = new ArrayList<>();

    /**
     * Constructs a new {@link NodeBuilder} identified by the named variable in the context of its enclosing Cypher
     * query.
     *
     * @param variableName The name of the variable to use
     */
    NodeBuilder(String variableName) {
        this.cypherReference = variableName;
    }

    NodeBuilder addLabel(String labelName) {
        this.labels.add(labelName);
        return this;
    }


    NodeBuilder addProperty(String propertyName, Object value) {
        this.props.put(propertyName, value);
        return this;
    }

    public NodeBuilder addLabels(Iterable<String> labelName) {
        for (String label : labelName) {
            addLabel(label);
        }
        return this;
    }

    public abstract NodeBuilder mapProperties(Object toPersist, ClassInfo classInfo, EntityAccessStrategy objectAccessStrategy);

    @Override
    public String toString() {
        return "(" + cypherReference + ":" + this.labels + " " + this.props + ")";
    }

    public static String toCsv(Iterable<String> elements) {
        StringBuilder sb = new StringBuilder();
        for (String element : elements) {
            sb.append(element).append(',');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public String reference() {
        return cypherReference;
    }

    @Override
    public int compareTo(NodeBuilder o) {
        return cypherReference.compareTo(o.cypherReference);
    }


}
