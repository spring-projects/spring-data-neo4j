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

import org.neo4j.ogm.cypher.query.GraphModelQuery;
import org.neo4j.ogm.exception.InvalidDepthException;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Utils;

import java.util.Collection;

/**
 * @author Luanne Misquitta
 * @author Luanne Misquitta
 */
public class VariableDepthRelationshipQuery implements QueryStatements {
    @Override
    public GraphModelQuery findOne(Long id, int depth) {
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
    public GraphModelQuery findAll(Collection<Long> ids, int depth) {
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
    public GraphModelQuery findAll() {
        return new GraphModelQuery("MATCH p=()-->() RETURN p", Utils.map());
    }

    @Override
    public GraphModelQuery findByType(String type, int depth) {
        int max = max(depth);
        int min = min(max);
        if (max > 0) {
            String qry = String.format("MATCH p=(n)-[:`%s`*%d..%d]-(m) RETURN collect(distinct p)", type, min, max);
            return new GraphModelQuery(qry, Utils.map());
        } else {
            throw new InvalidDepthException("Cannot load a relationship entity with depth 0 i.e. no start or end node");
        }
    }

    @Override
    public GraphModelQuery findByProperty(String type, Property<String, Object> property, int depth) {
        int max = max(depth);
        int min = min(max);
        if (max > 0) {
            String qry = String.format("MATCH (n)-[r:`%s`]->() WHERE r.%s = { %s } WITH n MATCH p=(n)-[*%d..%d]-(m)  RETURN collect(distinct p)", type, property.getKey(), property.getKey(), min, max);
            return new GraphModelQuery(qry, Utils.map(property.getKey(), property.asParameter()));
        } else {
            throw new InvalidDepthException("Cannot load a relationship entity with depth 0 i.e. no start or end node");
        }
    }

    private int min(int depth) {
        return Math.min(0, depth);
    }

    private int max(int depth) {
        return Math.max(0, depth);
    }


}
