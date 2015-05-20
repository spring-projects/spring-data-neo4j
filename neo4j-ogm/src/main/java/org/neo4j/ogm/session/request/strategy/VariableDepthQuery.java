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

package org.neo4j.ogm.session.request.strategy;


import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.*;
import org.neo4j.ogm.session.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class VariableDepthQuery implements QueryStatements {

    @Override
    public Query findOne(Long id, int depth) {
        int max = max(depth);
        int min = min(max);
        if (depth < 0) {
            return InfiniteDepthReadStrategy.findOne(id);
        }
        if (max > 0) {
            String qry = String.format("MATCH (n) WHERE id(n) = { id } WITH n MATCH p=(n)-[*%d..%d]-(m) RETURN collect(distinct p)", min, max);
            return new GraphModelQuery(qry, Utils.map("id", id));
        } else {
            return DepthZeroReadStrategy.findOne(id);
        }
    }

    @Override
    public Query findAll(Collection<Long> ids, int depth) {
        int max = max(depth);
        int min = min(max);
        if (depth < 0) {
            return InfiniteDepthReadStrategy.findAll(ids);
        }
        if (max > 0) {
            String qry=String.format("MATCH (n) WHERE id(n) in { ids } WITH n MATCH p=(n)-[*%d..%d]-(m) RETURN collect(distinct p)", min, max);
            return new GraphModelQuery(qry, Utils.map("ids", ids));
        } else {
            return DepthZeroReadStrategy.findAll(ids);
        }
    }

    @Override
    public Query findAll() {
        return new GraphModelQuery("MATCH p=()-->() RETURN p", Utils.map());
    }

    @Override
    public Query findByType(String label, int depth) {
        int max = max(depth);
        int min = min(max);
        if (depth < 0) {
            return InfiniteDepthReadStrategy.findByLabel(label);
        }
        if (max > 0) {
            String qry = String.format("MATCH (n:`%s`) WITH n MATCH p=(n)-[*%d..%d]-(m) RETURN collect(distinct p)", label, min, max);
            return new GraphModelQuery(qry, Utils.map());
        } else {
            return DepthZeroReadStrategy.findByLabel(label);
        }
    }

    @Override
    public Query findByProperties(String label, Filters parameters, int depth) {
        int max = max(depth);
        int min = min(max);
        if (depth < 0) {
            return InfiniteDepthReadStrategy.findByProperties(label, parameters);
        }
        if (max > 0) {
            Map<String,Object> properties = new HashMap<>();
            StringBuilder query = constructQuery(label, parameters, properties);
            query.append(String.format("WITH n MATCH p=(n)-[*%d..%d]-(m) RETURN collect(distinct p), ID(n)",min,max));
            return new GraphRowModelQuery(query.toString(), properties);
        } else {
            return DepthZeroReadStrategy.findByProperties(label, parameters);
        }
    }

    private static StringBuilder constructQuery(String label, Filters parameters, Map<String, Object> properties) {
        StringBuilder query = new StringBuilder(String.format("MATCH (n:`%s`)",label));
        StringBuilder relationshipMatch = null;
        for(Filter parameter : parameters) {
            if(parameter.isNested()) {
                if(parameter.getBooleanOperator().equals(BooleanOperator.OR)) {
                    query.append(" OPTIONAL");
                }
                query.append(String.format(" MATCH (x:`%s`)",parameter.getNestedEntityTypeLabel()));
                relationshipMatch = constructRelationshipMatch(parameter);
                parameter.setBooleanOperator(BooleanOperator.NONE); //todo this will change once we support more complex patterns
            }
			if(!parameter.getBooleanOperator().equals(BooleanOperator.NONE)) {
				query.append(parameter.getBooleanOperator().getValue());
			}
            else {
                query.append(" WHERE");
            }
			query.append(String.format(" %s.`%s` %s { `%s` } ",parameter.isNested() ? "x":"n",parameter.getPropertyName(), parameter.getComparisonOperator().getValue(), parameter.getPropertyName()));
			properties.put(parameter.getPropertyName(),parameter.getPropertyValue());
		}
        if(relationshipMatch != null) {
            query.append(relationshipMatch);
        }
        return query;
    }

    private static StringBuilder constructRelationshipMatch(Filter parameter) {
        StringBuilder relationshipMatch;
        relationshipMatch = new StringBuilder();
        if(parameter.getBooleanOperator().equals(BooleanOperator.OR)) {
			relationshipMatch.append(" OPTIONAL");
		}
        relationshipMatch.append(" MATCH (n)");
        if(parameter.getRelationshipDirection().equals(Relationship.INCOMING)) {
			relationshipMatch.append("<");
		}
        relationshipMatch.append(String.format("-[:`%s`]-", parameter.getRelationshipType()));
        if(parameter.getRelationshipDirection().equals(Relationship.OUTGOING)) {
			relationshipMatch.append(">");
		}
        relationshipMatch.append("(x) ");
        return relationshipMatch;
    }

    private int min(int depth) {
        return Math.min(0, depth);
    }

    private int max(int depth) {
        return Math.max(0, depth);
    }

    private static class DepthZeroReadStrategy {

        public static GraphModelQuery findOne(Long id) {
            return new GraphModelQuery("MATCH (n) WHERE id(n) = { id } RETURN n", Utils.map("id", id));
        }

        public static GraphModelQuery findAll(Collection<Long> ids) {
            return new GraphModelQuery("MATCH (n) WHERE id(n) in { ids } RETURN collect(n)", Utils.map("ids", ids));
        }

        public static GraphModelQuery findByLabel(String label) {
            return new GraphModelQuery(String.format("MATCH (n:`%s`) RETURN collect(n)", label), Utils.map());
        }

        public static GraphRowModelQuery findByProperties(String label, Filters parameters) {
            Map<String,Object> properties = new HashMap<>();
            StringBuilder query = constructQuery(label, parameters, properties);
            query.append("WITH n MATCH p=(n)-[*0..0]-(m) RETURN collect(distinct p), ID(n)");
            return new GraphRowModelQuery(query.toString(), properties);
        }

    }

    private static class InfiniteDepthReadStrategy {

        public static GraphModelQuery findOne(Long id) {
            return new GraphModelQuery("MATCH p=(n)-[*0..]-(m) WHERE id(n) = { id } RETURN collect(distinct p)", Utils.map("id", id));
        }

        public static GraphModelQuery findAll(Collection<Long> ids) {
            return new GraphModelQuery("MATCH p=(n)-[*0..]-(m) WHERE id(n) in { ids } RETURN collect(distinct p)", Utils.map("ids", ids));
        }

        public static GraphModelQuery findByLabel(String label) {
            return new GraphModelQuery(String.format("MATCH p=(n:`%s`)-[*0..]-(m) RETURN collect(distinct p)", label), Utils.map());
        }

        public static GraphRowModelQuery findByProperties(String label, Filters parameters) {
            Map<String,Object> properties = new HashMap<>();
            StringBuilder query = constructQuery(label, parameters, properties);
            query.append(" WITH n MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p), ID(n)");
            return new GraphRowModelQuery(query.toString(), properties);
        }

    }
}
