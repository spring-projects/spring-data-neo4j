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
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) IN { ids } WITH n SKIP 30 LIMIT 10 MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), 1).setPage(new Pagination(3, 10)).getStatement());
    }

    @Test
    public void testFindAll() throws Exception {
        assertEquals("MATCH p=()-->() WITH p SKIP 2 LIMIT 2 RETURN p", query.findAll().setPage(new Pagination(1, 2)).getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p SKIP 10 LIMIT 10 RETURN collect(distinct p)", query.findByType("ORBITS", 3).setPage(new Pagination(1, 10)).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        assertEquals("MATCH (n)-[r:`ORBITS`]->() WHERE r.`distance` = { `distance` } WITH n,r SKIP 0 LIMIT 4 MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(r)", query.findByProperties("ORBITS", new Filters().add(new Filter("distance", 60.2)), 1).setPage(new Pagination(0, 4)).getStatement());
    }

}
