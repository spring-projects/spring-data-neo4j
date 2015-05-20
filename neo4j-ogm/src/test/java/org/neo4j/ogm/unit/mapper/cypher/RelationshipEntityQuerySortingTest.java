package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.session.request.strategy.QueryStatements;
import org.neo4j.ogm.session.request.strategy.VariableDepthRelationshipQuery;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author: Vince Bickers
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
        sortOrder.add("r.distance");
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) IN { ids } WITH n,r ORDER BY r.distance MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), 1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindAll() throws Exception {
        sortOrder.add("length(p)");
        assertEquals("MATCH p=()-->() WITH p ORDER BY length(p) RETURN p", query.findAll().setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        sortOrder.add("r.distance");
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p,r ORDER BY r.distance RETURN collect(distinct p)", query.findByType("ORBITS", 3).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        filters.add("distance", 60.2);
        sortOrder.add("r.aphelion");
        assertEquals("MATCH (n)-[r:`ORBITS`]->() WHERE r.`distance` = { `distance` } WITH n,r ORDER BY r.aphelion MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(r)", query.findByProperties("ORBITS", filters, 1).setSortOrder(sortOrder).getStatement());
    }

}
