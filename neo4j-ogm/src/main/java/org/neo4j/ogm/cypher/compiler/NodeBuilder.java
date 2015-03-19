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

package org.neo4j.ogm.cypher.compiler;

import org.neo4j.ogm.entityaccess.EntityAccessStrategy;
import org.neo4j.ogm.metadata.info.ClassInfo;

import java.util.*;

/**
 * @author Vince Bickers
 */
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
