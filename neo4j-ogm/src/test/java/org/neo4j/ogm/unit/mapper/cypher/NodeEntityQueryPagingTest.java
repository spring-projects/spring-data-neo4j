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
import org.neo4j.ogm.cypher.query.Query;
import org.neo4j.ogm.cypher.query.RowModelQuery;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.session.request.strategy.VariableDepthQuery;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * @author Vince Bickers
 * @author Rene Richter
 */
public class NodeEntityQueryPagingTest {

    private final VariableDepthQuery variableDepthQuery = new VariableDepthQuery();

    @Test
    public void testFindAll() {
        check("MATCH p=()-->() RETURN p SKIP 2 LIMIT 2", variableDepthQuery.findAll().setPagination(new Pagination(1, 2)).getStatement());
    }

    @Test
    public void testFindById() {
        check("MATCH (n) WHERE id(n) in { ids } WITH n SKIP 2 LIMIT 2 MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", variableDepthQuery.findAll(Arrays.asList(23L, 24L), 1).setPagination(new Pagination(1, 2)).getStatement());
    }

    @Test
    public void testFindByType() {
        check("MATCH (n:`Raptor`) WITH n SKIP 4 LIMIT 2 MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", variableDepthQuery.findByType("Raptor", 1).setPagination(new Pagination(2, 2)).getStatement());
    }

    @Test
    public void testFindByProperty() {
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` } WITH n SKIP 0 LIMIT 2 MATCH p=(n)-[*0..2]-(m) RETURN collect(distinct p), ID(n)", variableDepthQuery.findByProperties("Raptor", new Filters().add(new Filter("name", "velociraptor")), 2).setPagination(new Pagination(0, 2)).getStatement());
    }

    @Test
    public void testFindByIdDepthZero() {
        check("MATCH (n) WHERE id(n) in { ids } RETURN n SKIP 1 LIMIT 1", variableDepthQuery.findAll(Arrays.asList(23L, 24L), 0).setPagination(new Pagination(1, 1)).getStatement());
    }

    @Test
    public void testFindByTypeDepthZero() {
        check("MATCH (n:`Raptor`) RETURN n SKIP 4 LIMIT 2", variableDepthQuery.findByType("Raptor", 0).setPagination(new Pagination(2, 2)).getStatement());
    }

    @Test
    public void testByPropertyDepthZero() {
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` } RETURN n SKIP 0 LIMIT 2", variableDepthQuery.findByProperties("Raptor", new Filters().add(new Filter("name", "velociraptor")), 0).setPagination(new Pagination(0, 2)).getStatement());
    }

    @Test
    public void testFindByIdDepthInfinite() {
        check("MATCH (n) WHERE id(n) in { ids } WITH n SKIP 2 LIMIT 2 MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p)", variableDepthQuery.findAll(Arrays.asList(23L, 24L), -1).setPagination(new Pagination(1, 2)).getStatement());
    }

    @Test
    public void testFindByTypeDepthInfinite() {
        check("MATCH (n:`Raptor`) WITH n SKIP 6 LIMIT 2 MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p)", variableDepthQuery.findByType("Raptor", -1).setPagination(new Pagination(3, 2)).getStatement());
    }

    @Test
    public void testFindByPropertyDepthInfinite() {
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` }  WITH n SKIP 0 LIMIT 2 MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p), ID(n)", variableDepthQuery.findByProperties("Raptor", new Filters().add(new Filter("name", "velociraptor")), -1).setPagination(new Pagination(0, 2)).getStatement());
    }


    @Test
    public void itShouldTakeTheFirstNodeAliasForSorting() {
        String cypher = "match (u)->[a:HAS_MANY]->(t:Tupel{id:2}) RETURN u";
        Query query = new RowModelQuery(cypher,new HashMap<String,Object>());
        query.setPagination(new Pagination(0, 2));
        query.setSortOrder(new SortOrder().add("firstname").add("lastname"));

        check("match (u)->[a:HAS_MANY]->(t:Tupel{id:2}) WITH u ORDER BY u.firstname,u.lastname SKIP 0 LIMIT 2 RETURN u",query.getStatement());
    }


    @Test
    public void itShouldTakeRelationshipAliasIfFirstAliasIsRelatinshipAlias() {
        String cypher = "match ()->[a:HAS_MANY]->(t:Tupel{id:2}) RETURN a";
        Query query = new RowModelQuery(cypher,new HashMap<String,Object>());
        query.setPagination(new Pagination(0, 2));
        query.setSortOrder(new SortOrder().add("date"));

        check("match ()->[a:HAS_MANY]->(t:Tupel{id:2}) WITH a ORDER BY a.date SKIP 0 LIMIT 2 RETURN a",query.getStatement());
    }

    @Test
    public void itShouldNotCreateAWithClauseIfNoSortIsPresentAndNoWithClauseWasSpecified() {
        String cypher = "match xyz=()-->() RETURN xyz";
        Query query = new RowModelQuery(cypher,new HashMap<String,Object>());
        query.setPagination(new Pagination(0, 2));

        check("match xyz=()-->() RETURN xyz SKIP 0 LIMIT 2",query.getStatement());
    }

    private void check(String expected, String actual) {
        assertEquals(expected, actual);
    }
}
