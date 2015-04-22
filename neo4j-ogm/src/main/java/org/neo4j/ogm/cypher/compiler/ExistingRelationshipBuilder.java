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
import java.util.Set;

/**
 * @author Adam George
 * @author Vince Bickers
 */
class ExistingRelationshipBuilder extends RelationshipBuilder {

    ExistingRelationshipBuilder(String variableName, Long relationshipId) {
        super(variableName);
        setId(relationshipId);
    }

    @Override
    public void relate(String startNodeIdentifier, String endNodeIdentifier) {
        this.startNodeIdentifier = startNodeIdentifier;
        this.endNodeIdentifier = endNodeIdentifier;
        this.reference = reference;
    }

    @Override
    public boolean isNew() {
        return false;
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
                .append(this.reference).append(")=").append(this.getId());

        if (!this.props.isEmpty()) {
            queryBuilder.append(" SET ").append(this.reference).append("+={").append(this.reference).append("_props} ");
            parameters.put(this.reference + "_props", this.props);
            varStack.add(this.reference);
        }

        return true;
    }

}
