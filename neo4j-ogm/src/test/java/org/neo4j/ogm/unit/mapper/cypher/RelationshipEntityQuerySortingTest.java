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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.session.request.strategy.QueryStatements;
import org.neo4j.ogm.session.request.strategy.VariableDepthRelationshipQuery;

/**
 * @author Vince Bickers
 */
public class RelationshipEntityQuerySortingTest {
    
    private final QueryStatements query = new VariableDepthRelationshipQuery();
    private SortOrder sortOrder;
    private Filters filters;

    @Before
    public void setUp() {
        sortOrder = new SortOrder();
        filters = new Filters();
    }

    @Test
    public void testFindAllCollection() throws Exception {
        sortOrder.add("distance");
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) IN { ids } WITH n,r ORDER BY r.distance MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), 1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        sortOrder.add("distance");
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p,r ORDER BY r.distance RETURN collect(distinct p)", query.findByType("ORBITS", 3).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        filters.add("distance", 60.2);
        sortOrder.add("aphelion");
        assertEquals("MATCH (n)-[r:`ORBITS`]->() WHERE r.`distance` = { `distance` } WITH n,r ORDER BY r.aphelion MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(r)", query.findByProperties("ORBITS", filters, 1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testMultipleSortOrders() {
        sortOrder.add(SortOrder.Direction.DESC, "distance", "aphelion");
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p,r ORDER BY r.distance,r.aphelion DESC RETURN collect(distinct p)", query.findByType("ORBITS", 3).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testDifferentSortDirections() {
        sortOrder.add(SortOrder.Direction.DESC, "type").add("name");
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p,r ORDER BY r.type DESC,r.name RETURN collect(distinct p)", query.findByType("ORBITS", 3).setSortOrder(sortOrder).getStatement());
    }

}
