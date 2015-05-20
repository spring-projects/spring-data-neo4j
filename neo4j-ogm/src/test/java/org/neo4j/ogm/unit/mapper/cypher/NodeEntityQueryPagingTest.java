package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.Test;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.session.request.strategy.VariableDepthQuery;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author: Vince Bickers
 */
public class NodeEntityQueryPagingTest {

    private final VariableDepthQuery query = new VariableDepthQuery();

    @Test
    public void testFindAll() {
        check("MATCH p=()-->() WITH p SKIP 2 LIMIT 2 RETURN p", query.findAll().setPage(new Pagination(1, 2)).getStatement());
    }

    @Test
    public void testFindById() {
        check("MATCH (n) WHERE id(n) in { ids } WITH n SKIP 2 LIMIT 2 MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(23L, 24L), 1).setPage(new Pagination(1, 2)).getStatement());
    }

    @Test
    public void testFindByType() {
        check("MATCH (n:`Raptor`) WITH n SKIP 4 LIMIT 2 MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findByType("Raptor", 1).setPage(new Pagination(2,2)).getStatement());
    }

    @Test
    public void testFindByProperty() {
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` } WITH n SKIP 0 LIMIT 2 MATCH p=(n)-[*0..2]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Raptor", new Filters().add(new Filter("name", "velociraptor")), 2).setPage(new Pagination(0, 2)).getStatement());
    }

    @Test
    public void testFindByIdDepthZero() {
        check("MATCH (n) WHERE id(n) in { ids } WITH n SKIP 1 LIMIT 1 RETURN collect(n)", query.findAll(Arrays.asList(23L, 24L), 0).setPage(new Pagination(1, 1)).getStatement());
    }

    @Test
    public void testFindByTypeDepthZero() {
        check("MATCH (n:`Raptor`) WITH n SKIP 4 LIMIT 2 RETURN collect(n)", query.findByType("Raptor",0).setPage(new Pagination(2,2)).getStatement());
    }

    @Test
    public void testByPropertyDepthZero() {
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` } WITH n SKIP 0 LIMIT 2 MATCH p=(n)-[*0..0]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Raptor", new Filters().add(new Filter("name", "velociraptor")), 0).setPage(new Pagination(0, 2)).getStatement());
    }

    @Test
    public void testFindByIdDepthInfinite() {
        check("MATCH p=(n)-[*0..]-(m) WHERE id(n) in { ids } WITH p SKIP 2 LIMIT 2 RETURN collect(distinct p)", query.findAll(Arrays.asList(23L, 24L), -1).setPage(new Pagination(1, 2)).getStatement());
    }

    @Test
    public void testFindByTypeDepthInfinite() {
        check("MATCH p=(n:`Raptor`)-[*0..]-(m) WITH p SKIP 6 LIMIT 2 RETURN collect(distinct p)", query.findByType("Raptor", -1).setPage(new Pagination(3,2)).getStatement());
    }

    @Test
    public void testFindByPropertyDepthInfinite() {
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` }  WITH n SKIP 0 LIMIT 2 MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Raptor", new Filters().add(new Filter("name", "velociraptor")), -1).setPage(new Pagination(0, 2)).getStatement());
    }

    private void check(String expected, String actual) {
        assertEquals(expected, actual);
    }
}
