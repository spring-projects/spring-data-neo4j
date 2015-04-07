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

import java.util.Collection;

import org.neo4j.ogm.cypher.query.GraphModelQuery;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Utils;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class VariableDepthQuery implements QueryStatements {

    @Override
    public GraphModelQuery findOne(Long id, int depth) {
        int max = max(depth);
        int min = min(max);
        if (depth < 0) {
            return InfiniteDepthReadStrategy.findOne(id);
        }
        if (max > 0) {
            String qry = String.format("MATCH p=(n)-[*%d..%d]-(m) WHERE id(n) = { id } RETURN collect(distinct p)", min, max);
            return new GraphModelQuery(qry, Utils.map("id", id));
        } else {
            return DepthZeroReadStrategy.findOne(id);
        }
    }

    @Override
    public GraphModelQuery findAll(Collection<Long> ids, int depth) {
        int max = max(depth);
        int min = min(max);
        if (depth < 0) {
            return InfiniteDepthReadStrategy.findAll(ids);
        }
        if (max > 0) {
            String qry=String.format("MATCH p=(n)-[*%d..%d]-(m) WHERE id(n) in { ids } RETURN collect(distinct p)", min, max);
            return new GraphModelQuery(qry, Utils.map("ids", ids));
        } else {
            return DepthZeroReadStrategy.findAll(ids);
        }
    }

    @Override
    public GraphModelQuery findAll() {
        return new GraphModelQuery("MATCH p=()-->() RETURN p", Utils.map());
    }

    @Override
    public GraphModelQuery findByType(String label, int depth) {
        int max = max(depth);
        int min = min(max);
        if (depth < 0) {
            return InfiniteDepthReadStrategy.findByLabel(label);
        }
        if (max > 0) {
            String qry = String.format("MATCH p=(n:`%s`)-[*%d..%d]-(m) RETURN collect(distinct p)", label, min, max);
            return new GraphModelQuery(qry, Utils.map());
        } else {
            return DepthZeroReadStrategy.findByLabel(label);
        }
    }

    @Override
    public GraphModelQuery findByProperty(String label, Property<String, Object> property, int depth) {
        int max = max(depth);
        int min = min(max);
        if (depth < 0) {
            return InfiniteDepthReadStrategy.findByProperty(label, property);
        }
        if (max > 0) {
            String qry = String.format("MATCH p=(n:`%s`)-[*%d..%d]-(m) WHERE n.%s = { %s } RETURN collect(distinct p)", label, min, max, property.getKey(), property.getKey());
            return new GraphModelQuery(qry, Utils.map(property.getKey(), property.asParameter()));
        } else {
            return DepthZeroReadStrategy.findByProperty(label, property);
        }
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

        public static GraphModelQuery findByProperty(String label, Property<String, Object> property) {
            return new GraphModelQuery(String.format("MATCH (n:`%s`) WHERE n.%s = { %s } RETURN collect(n)", label, property.getKey(), property.getKey()), Utils.map(property.getKey(), property.asParameter()));
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

        public static GraphModelQuery findByProperty(String label, Property<String, Object> property) {
            return new GraphModelQuery(String.format("MATCH p=(n:`%s`)-[*0..]-(m) WHERE n.%s = { %s } RETURN collect(distinct p)", label, property.getKey(), property.getKey()), Utils.map(property.getKey(), property.asParameter()));
        }

    }
}
