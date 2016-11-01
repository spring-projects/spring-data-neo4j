/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.repository.query.derived;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.springframework.data.neo4j.repository.query.derived.builder.*;
import org.springframework.data.repository.query.parser.Part;

/**
 * A {@link DerivedQueryDefinition} that builds a Cypher query.
 *
 * @author Luanne Misquitta
 * @author Jasper Blues
 */
public class CypherFinderQuery implements DerivedQueryDefinition {

    private Class<?> entityType;
    private Part basePart;
    private List<CypherFilter> cypherFilters = new ArrayList<>();
    private int paramPosition = 0;

    public CypherFinderQuery(Class<?> entityType, Part basePart) {
        this.entityType = entityType;
        this.basePart = basePart;
    }

    @Override
    public Part getBasePart() { //because the OR is handled in a weird way. Luanne, explain better
        return basePart;
    }

    @Override
    public List<CypherFilter> getCypherFilters() {
        return cypherFilters;
    }

    @Override
    public void addPart(Part part, BooleanOperator booleanOperator) {

        List<CypherFilter> filters = builderForPart(part, booleanOperator).build();
        for (CypherFilter filter : filters) {
            filter.setPropertyPosition(paramPosition);
            cypherFilters.add(filter);
            paramPosition += filter.functionAdapter.parameterCount();
        }
    }

    private CypherFilterBuilder builderForPart(Part part, BooleanOperator booleanOperator) {
        switch (part.getType()) {
            case NEAR:
                return new DistanceComparisonFilterBuilder(part, booleanOperator, entityType);
            case BETWEEN:
                return new BetweenComparisonBuilder(part, booleanOperator, entityType);
            case IS_NULL:
            case IS_NOT_NULL:
                return new IsNullFilterBuilder(part, booleanOperator, entityType);
            case EXISTS:
                return new ExistsFilterBuilder(part, booleanOperator, entityType);
            default:
                return new PropertyComparisonFilterBuilder(part, booleanOperator, entityType);
        }
    }
}
