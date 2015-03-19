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

import org.neo4j.ogm.cypher.statement.ParameterisedStatement;

import java.util.*;
import java.util.Map.Entry;

/**
 * Implementation of {@link CypherCompiler} that builds a single query for the object graph.
 */
public class SingleStatementCypherCompiler implements CypherCompiler {

    private final IdentifierManager identifiers = new IdentifierManager();

    private final Set<CypherEmitter> newNodes = new TreeSet<>();
    private final Set<CypherEmitter> updatedNodes = new TreeSet<>();
    private final Set<CypherEmitter> newRelationships = new TreeSet<>();
    private final Set<CypherEmitter> updatedRelationships = new TreeSet<>();
    private final Set<CypherEmitter> deletedRelationships = new TreeSet<>();

    private final CypherEmitter returnClause = new ReturnClauseBuilder();
    private final CypherContext context = new CypherContext();

    @Deprecated
    @Override
    public void relate(String startNode, String relationshipType, Map<String, Object> relationshipProperties, String endNode) {
        RelationshipBuilder newRelationship = newRelationship();
        newRelationship.type(relationshipType);
        for (Entry<String, Object> property : relationshipProperties.entrySet()) {
            newRelationship.addProperty(property.getKey(), property.getValue());
        }
        newRelationship.relate(startNode, endNode);
        newRelationships.add(newRelationship);
    }

    @Override
    public void unrelate(String startNode, String relationshipType, String endNode) {
        deletedRelationships.add(new DeletedRelationshipBuilder(relationshipType,startNode, endNode, this.identifiers.nextIdentifier()));
    }

    @Override
    public NodeBuilder newNode() {
        NodeBuilder newNode = new NewNodeBuilder(this.identifiers.nextIdentifier());
        this.newNodes.add(newNode);
        return newNode;
    }

    @Override
    public NodeBuilder existingNode(Long existingNodeId) {
        NodeBuilder node = new ExistingNodeBuilder(this.identifiers.identifier(existingNodeId));
        this.updatedNodes.add(node);
        return node;
    }

    @Override
    public RelationshipBuilder newRelationship() {
        RelationshipBuilder builder = new NewRelationshipBuilder(identifiers.nextIdentifier());
        this.newRelationships.add(builder);
        return builder;
    }

    @Override
    public RelationshipBuilder existingRelationship(Long existingRelationshipId) {
        RelationshipBuilder builder = new ExistingRelationshipBuilder(this.identifiers.nextIdentifier(), existingRelationshipId);
        this.updatedRelationships.add(builder);
        return builder;
    }

    @Override
    public List<ParameterisedStatement> getStatements() {

        StringBuilder queryBuilder = new StringBuilder();

        Set<String> varStack = new TreeSet<>();
        Set<String> newStack = new TreeSet<>();

        Map<String, Object> parameters = new HashMap<>();

        // all create statements can be done in a single clause.
        if (! this.newNodes.isEmpty() ) {
            queryBuilder.append(" CREATE ");
            for (Iterator<CypherEmitter> it = this.newNodes.iterator() ; it.hasNext() ; ) {
                NodeBuilder node = (NodeBuilder) it.next();
                if (node.emit(queryBuilder, parameters, varStack)) {
                    newStack.add(node.reference());  // for the return clause
                    if (it.hasNext()) {
                          queryBuilder.append(", ");
                    }
                }
            }
        }

        for (CypherEmitter emitter : updatedNodes) {
            emitter.emit(queryBuilder, parameters, varStack);
        }


        for (Iterator<CypherEmitter> it = this.newRelationships.iterator() ; it.hasNext() ; ) {
            RelationshipBuilder relationshipBuilder = (RelationshipBuilder) it.next();
            if (relationshipBuilder.emit(queryBuilder, parameters, varStack)) {
                newStack.add(relationshipBuilder.reference);
            }
        }

        for (CypherEmitter emitter : updatedRelationships) {
            emitter.emit(queryBuilder, parameters, varStack);
        }

        for (CypherEmitter emitter : deletedRelationships) {
            emitter.emit(queryBuilder, parameters, varStack);
        }

        returnClause.emit(queryBuilder, parameters, newStack);

        return Collections.singletonList(new ParameterisedStatement(queryBuilder.toString(), parameters));
    }

    public CypherContext context() {
        return context;
    }

    @Override
    public CypherContext compile() {
        context.setStatements(getStatements());
        return context;
    }

    @Override
    public void release(RelationshipBuilder relationshipBuilder) {
        identifiers.releaseIdentifier();
    }

    @Override
    public String nextIdentifier() {
        return identifiers.nextIdentifier();
    }


}
