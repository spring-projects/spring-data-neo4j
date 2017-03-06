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

import java.util.*;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.neo4j.repository.query.derived.builder.*;
import org.springframework.data.repository.query.parser.Part;

/**
 * A {@link DerivedQueryDefinition} that builds a Cypher query.
 *
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 */
public class CypherFinderQuery implements DerivedQueryDefinition {

    private Class<?> entityType;
    private Part basePart;
    private List<FilterBuilder> filterBuilders = new ArrayList<>();

    CypherFinderQuery(Class<?> entityType, Part basePart) {
        this.entityType = entityType;
        this.basePart = basePart;
    }

    @Override
    public Part getBasePart() { //because the OR is handled in a weird way. Luanne, explain better
        return basePart;
    }

    @Override
    public List<Filter> getFilters(Map<Integer, Object> params) {

        // buiding a stack of parameter values, so that the builders can pull them
        // according to their needs (zero, one or more parameters)
        // avoids to manage a current parameter index state here.
        Stack<Object> parametersStack = new Stack<>();
        if (!params.isEmpty()) {
            Integer maxParameterIndex = Collections.max(params.keySet());
            for (int i = 0; i <= maxParameterIndex; i++) {
                parametersStack.add(0, params.get(i));
            }
        }

        List<Filter> filters = new ArrayList<>();
        for (FilterBuilder filterBuilder : filterBuilders) {
            filters.addAll(filterBuilder.build(parametersStack));
        }
        return filters;
    }

    @Override
    public void addPart(Part part, BooleanOperator booleanOperator) {

        FilterBuilder builder = builderForPart(part, booleanOperator);
        filterBuilders.add(builder);
    }

    private FilterBuilder builderForPart(Part part, BooleanOperator booleanOperator) {
        switch (part.getType()) {
            case NEAR:
                return new DistanceComparisonBuilder(part, booleanOperator, entityType);
            case BETWEEN:
                return new BetweenComparisonBuilder(part, booleanOperator, entityType);
            case IS_NULL:
            case IS_NOT_NULL:
                return new IsNullFilterBuilder(part, booleanOperator, entityType);
            case EXISTS:
                return new ExistsFilterBuilder(part, booleanOperator, entityType);
            case TRUE:
            case FALSE:
                return new BooleanComparisonBuilder(part, booleanOperator, entityType);
            default:
                return new PropertyComparisonBuilder(part, booleanOperator, entityType);
        }
    }
}
