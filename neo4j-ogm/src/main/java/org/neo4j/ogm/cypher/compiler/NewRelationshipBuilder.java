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
