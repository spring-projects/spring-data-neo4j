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

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Parameter;
import org.neo4j.ogm.cypher.query.GraphModelQuery;
import org.neo4j.ogm.cypher.query.GraphRowModelQuery;
import org.neo4j.ogm.cypher.query.Paging;
import org.neo4j.ogm.cypher.query.Query;
import org.neo4j.ogm.exception.InvalidDepthException;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Luanne Misquitta
 */
public class VariableDepthRelationshipQuery implements QueryStatements {

    @Override
    public Query findOne(Long id, int depth) {
        int max = max(depth);
        int min = min(max);
        if (max > 0) {
            String qry = String.format("MATCH (n)-[r]->() WHERE ID(r) = { id } WITH n MATCH p=(n)-[*%d..%d]-(m) RETURN collect(distinct p)", min, max);
            return new GraphModelQuery(qry, Utils.map("id", id));
        } else {
            throw new InvalidDepthException("Cannot load a relationship entity with depth 0 i.e. no start or end node");
        }
    }

    @Override
    public Query findAll(Collection<Long> ids, int depth) {
        int max = max(depth);
        int min = min(max);
        if (max > 0) {
            String qry=String.format("MATCH (n)-[r]->() WHERE ID(r) IN { ids } WITH n MATCH p=(n)-[*%d..%d]-(m) RETURN collect(distinct p)", min, max);
            return new GraphModelQuery(qry, Utils.map("ids", ids));
        } else {
            throw new InvalidDepthException("Cannot load a relationship entity with depth 0 i.e. no start or end node");
        }
    }

    @Override
    public Query findAll(Collection<Long> ids, Paging paging, int depth) {
        return findAll(ids, depth).setPage(paging);
    }

    @Override
    public Query findAll(Collection<Long> ids, String orderings, int depth) {
        return null;
    }

    @Override
    public Query findAll(Collection<Long> ids, String orderings, Paging paging, int depth) {
        return null;
    }

    @Override
    public Query findAll() {
        return new GraphModelQuery("MATCH p=()-[r]->() RETURN p", Utils.map());
    }

    @Override
    public Query findAll(Paging paging) {
        return findAll().setPage(paging);
    }

    @Override
    public Query findByType(String type, int depth) {
        int max = max(depth);
        if (max > 0) {
            String qry = String.format("MATCH p=()-[r:`%s`*..%d]-(m) RETURN collect(distinct p)", type, max);
            return new GraphModelQuery(qry, Utils.map());
        } else {
            throw new InvalidDepthException("Cannot load a relationship entity with depth 0 i.e. no start or end node");
        }
    }

    @Override
    public Query findByType(String type, String orderings, int depth) {
        return null;
    }

    @Override
    public Query findByType(String type, Paging paging, int depth) {
        return findByType(type, depth).setPage(paging);
    }

    @Override
    public Query findByType(String type, String orderings, Paging paging, int depth) {
        return null;
    }

	@Override
	public Query findByProperties(String type, Collection<Parameter> parameters, int depth) {
		int max = max(depth);
		int min = min(max);
		if (max > 0) {
			Map<String, Object> properties = new HashMap<>();
			StringBuilder query = new StringBuilder(String.format("MATCH (n)-[r:`%s`]->() WHERE", type));
			for (Parameter parameter : parameters) {
				if (parameter.getBooleanOperator() != BooleanOperator.NONE) {
					query.append(parameter.getBooleanOperator().getValue());
				}
				query.append(String.format(" r.`%s` %s { `%s` } ", parameter.getPropertyName(), parameter.getComparisonOperator().getValue(), parameter.getPropertyName()));
				properties.put(parameter.getPropertyName(), parameter.getPropertyValue());
			}
			query.append(String.format("WITH n,r MATCH p=(n)-[*%d..%d]-(m) RETURN collect(distinct p),ID(r)", min, max));
			return new GraphRowModelQuery(query.toString(), properties);
		} else {
			throw new InvalidDepthException("Cannot load a relationship entity with depth 0 i.e. no start or end node");
		}
	}

    @Override
    public Query findByProperties(String type, Collection<Parameter> parameters, String orderings, int depth) {
        return null;
    }

    @Override
    public Query findByProperties(String type, Collection<Parameter> parameters, Paging paging, int depth) {
        return null;
    }

    @Override
    public Query findByProperties(String type, Collection<Parameter> parameters, String orderings, Paging paging, int depth) {
        return null;
    }


    private int min(int depth) {
        return Math.min(0, depth);
    }

    private int max(int depth) {
		return Math.max(0, depth);
	}
}
