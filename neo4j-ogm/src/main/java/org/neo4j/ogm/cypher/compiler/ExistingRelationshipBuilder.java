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
import java.util.Set;

class ExistingRelationshipBuilder extends RelationshipBuilder {

    private final Long id;

    ExistingRelationshipBuilder(String variableName, Long relationshipId) {
        super(variableName);
        this.id = relationshipId;
    }

    @Override
    public void relate(String startNodeIdentifier, String endNodeIdentifier) {
        this.startNodeIdentifier = startNodeIdentifier;
        this.endNodeIdentifier = endNodeIdentifier;
        this.reference = reference;
    }

    @Override
    public boolean emit(StringBuilder queryBuilder, Map<String, Object> parameters, Set<String> varStack) {
        // admittedly, this isn't brilliant, as we'd ideally avoid creating the relationship in the first place
        // this doesn't make sense here because we don't even use the node identifiers for updating rels!
        if (this.startNodeIdentifier == null || this.endNodeIdentifier == null) {
            return false;
        }

        if (!varStack.isEmpty()) {
            queryBuilder.append(" WITH ").append(NodeBuilder.toCsv(varStack));
        }

        queryBuilder.append(" MATCH ()-[").append(this.reference).append("]->() WHERE id(")
                .append(this.reference).append(")=").append(this.id);

        if (!this.props.isEmpty()) {
            queryBuilder.append(" SET ").append(this.reference).append("+={").append(this.reference).append("_props} ");
            parameters.put(this.reference + "_props", this.props);
            varStack.add(this.reference);
        }

        return true;
    }

}
