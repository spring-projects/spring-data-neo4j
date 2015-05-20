package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.session.request.strategy.VariableDepthQuery;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author: Vince Bickers
 */
public class NodeEntityQuerySortingTest {

    private final VariableDepthQuery query = new VariableDepthQuery();

    private SortOrder sortOrder;
    private Filters filters;

    @Before
    public void setUp() {
        sortOrder = new SortOrder();
        filters = new Filters();
    }

    @Test
    public void testFindAll() {
        sortOrder.add("length(p)");
        check("MATCH p=()-->() WITH p ORDER BY length(p) RETURN p", query.findAll().setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindById() {
        sortOrder.add(SortOrder.Direction.DESC, "id(n)");
        check("MATCH (n) WHERE id(n) in { ids } WITH n ORDER BY id(n) DESC MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(23L, 24L), 1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByType() {
        sortOrder.add("n.name");
        check("MATCH (n:`Raptor`) WITH n ORDER BY n.name MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findByType("Raptor", 1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByProperty() {
        sortOrder.add(SortOrder.Direction.DESC, "n.weight");
        filters.add("name", "velociraptor");
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` } WITH n ORDER BY n.weight DESC MATCH p=(n)-[*0..2]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Raptor", filters, 2).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByIdDepthZero() {
        sortOrder.add("n.name");
        check("MATCH (n) WHERE id(n) in { ids } WITH n ORDER BY n.name RETURN n", query.findAll(Arrays.asList(23L, 24L), 0).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByTypeDepthZero() {
        sortOrder.add(SortOrder.Direction.DESC, "n.name");
        check("MATCH (n:`Raptor`) WITH n ORDER BY n.name DESC RETURN n", query.findByType("Raptor", 0).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testByPropertyDepthZero() {
        filters.add("name", "velociraptor");
        sortOrder.add(SortOrder.Direction.DESC, "n.weight");
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` } WITH n ORDER BY n.weight DESC RETURN n", query.findByProperties("Raptor", filters, 0).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByIdDepthInfinite() {
        sortOrder.add(SortOrder.Direction.DESC, "n.name");
        check("MATCH (n) WHERE id(n) in { ids } WITH n ORDER BY n.name DESC MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(23L, 24L), -1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByTypeDepthInfinite() {
        sortOrder.add(SortOrder.Direction.DESC, "n.name");
        check("MATCH (n:`Raptor`) WITH n ORDER BY n.name DESC MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p)", query.findByType("Raptor", -1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByPropertyDepthInfinite() {
        sortOrder.add(SortOrder.Direction.DESC, "n.name");
        filters.add("name", "velociraptor");
        check("MATCH (n:`Raptor`) WHERE n.`name` = { `name` }  WITH n ORDER BY n.name DESC MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Raptor", filters, -1).setSortOrder(sortOrder).getStatement());
    }

    private void check(String expected, String actual) {
        assertEquals(expected, actual);
    }
}
