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
package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.Test;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.session.request.strategy.QueryStatements;
import org.neo4j.ogm.session.request.strategy.VariableDepthRelationshipQuery;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author: Vince Bickers
 */
public class RelationshipEntityQueryPagingTest {
    
    private final QueryStatements query = new VariableDepthRelationshipQuery();

    @Test
    public void testFindAllCollection() throws Exception {
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) IN { ids } WITH n,r SKIP 30 LIMIT 10 MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), 1).setPagination(new Pagination(3, 10)).getStatement());
    }

    @Test
    public void testFindAll() throws Exception {
        assertEquals("MATCH p=()-->() WITH p SKIP 2 LIMIT 2 RETURN p", query.findAll().setPagination(new Pagination(1, 2)).getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p,r SKIP 10 LIMIT 10 RETURN collect(distinct p)", query.findByType("ORBITS", 3).setPagination(new Pagination(1, 10)).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        assertEquals("MATCH (n)-[r:`ORBITS`]->() WHERE r.`distance` = { `distance` } WITH n,r SKIP 0 LIMIT 4 MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(r)", query.findByProperties("ORBITS", new Filters().add(new Filter("distance", 60.2)), 1).setPagination(new Pagination(0, 4)).getStatement());
    }

}
