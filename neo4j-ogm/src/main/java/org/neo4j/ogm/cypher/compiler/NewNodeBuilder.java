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
import org.neo4j.ogm.entityaccess.PropertyReader;
import org.neo4j.ogm.metadata.info.ClassInfo;

import java.util.Map;
import java.util.Set;

/**
 * Renders Cypher appropriate for a new node that needs creating in the database.
 *
 * @author Vince Bickers
 */
class NewNodeBuilder extends NodeBuilder {

    NewNodeBuilder(String variableName) {
        super(variableName);
    }

    @Override
    public NodeBuilder mapProperties(Object toPersist, ClassInfo classInfo, EntityAccessStrategy objectAccessStrategy) {
        for (PropertyReader propertyReader : objectAccessStrategy.getPropertyReaders(classInfo)) {
            Object value = propertyReader.read(toPersist);
            if (value != null) {
                addProperty(propertyReader.propertyName(), value);
            }
        }
        return this;
    }

    @Override
    public boolean emit(StringBuilder queryBuilder, Map<String, Object> parameters, Set<String> varStack) {

        queryBuilder.append('(');
        queryBuilder.append(this.reference());
        for (String label : this.labels) {
            queryBuilder.append(":`").append(label).append('`');
        }
        if (!this.props.isEmpty()) {
            queryBuilder.append('{').append(this.reference()).append("_props}");
            parameters.put(this.reference() + "_props", this.props);
        }
        queryBuilder.append(')');
        varStack.add(this.reference());

        return true;
    }

}