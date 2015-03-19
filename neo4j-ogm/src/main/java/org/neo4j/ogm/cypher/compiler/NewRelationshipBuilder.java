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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class NewRelationshipBuilder extends RelationshipBuilder {

    public NewRelationshipBuilder(String reference) {
        super(reference);
    }

    @Override
    public void relate(String startNodeIdentifier, String endNodeIdentifier) {
        this.startNodeIdentifier = startNodeIdentifier;
        this.endNodeIdentifier = endNodeIdentifier;
    }

    @Override
    public boolean emit(StringBuilder queryBuilder, Map<String, Object> parameters, Set<String> varStack) {
        // don't emit anything if this relationship isn't used to link any nodes
        // admittedly, this isn't brilliant, as we'd ideally avoid creating the relationship in the first place
        if (this.startNodeIdentifier == null || this.endNodeIdentifier == null) {
            return false;
        }

        if (!varStack.isEmpty()) {
            queryBuilder.append(" WITH ").append(NodeBuilder.toCsv(varStack));
        }

        if (!varStack.contains(startNodeIdentifier)) {
            queryBuilder.append(" MATCH (");
            queryBuilder.append(startNodeIdentifier);
            queryBuilder.append(") WHERE id(");
            queryBuilder.append(startNodeIdentifier);
            queryBuilder.append(")=");
            queryBuilder.append(startNodeIdentifier.substring(1)); // existing nodes have an id. we pass it in as $id
            varStack.add(startNodeIdentifier);
        }

        if (!varStack.contains(endNodeIdentifier)) {
            queryBuilder.append(" MATCH (");
            queryBuilder.append(endNodeIdentifier);
            queryBuilder.append(") WHERE id(");
            queryBuilder.append(endNodeIdentifier);
            queryBuilder.append(")=");
            queryBuilder.append(endNodeIdentifier.substring(1)); // existing nodes have an id. we pass it in as $id
            varStack.add(endNodeIdentifier);
        }

        queryBuilder.append(" MERGE (");
        queryBuilder.append(startNodeIdentifier);
        queryBuilder.append(")-[").append(this.reference).append(":`");
        queryBuilder.append(type);
        queryBuilder.append('`');
        if (!this.props.isEmpty()) {
            queryBuilder.append('{');

            // for MERGE, we need properties in this format: name:{_#_props}.name
            final String propertyVariablePrefix = '{' + this.reference + "_props}.";
            for (Entry<String, Object> relationshipProperty: this.props.entrySet()) {
                if (relationshipProperty.getValue() != null) {
                    queryBuilder.append(relationshipProperty.getKey()).append(':')
                        .append(propertyVariablePrefix).append(relationshipProperty.getKey()).append(',');
                }
            }
            queryBuilder.setLength(queryBuilder.length() - 1);
            queryBuilder.append('}');

            parameters.put(this.reference + "_props", this.props);
        }
        queryBuilder.append("]->(");
        queryBuilder.append(endNodeIdentifier);
        queryBuilder.append(")");

        varStack.add(this.reference);

        return true;
    }





}
