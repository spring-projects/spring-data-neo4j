package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.Test;
import org.neo4j.ogm.cypher.query.Paging;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.request.strategy.VariableDepthQuery;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author: Vince Bickers
 */
public class NodeEntityQuerySortingTest {

    private final VariableDepthQuery query = new VariableDepthQuery();

    @Test
    public void testFindById() {
        check("MATCH p=(n)-[*0..1]-(m) WHERE id(n) in { ids } RETURN collect(distinct p) SKIP 1 LIMIT 1", query.findAll(Arrays.asList(23L, 24L), new Paging(1, 1), 1).getStatement());
    }

    @Test
    public void testFindByType() {
        check("MATCH p=(n:`Raptor`)-[*0..1]-(m) RETURN collect(distinct p) SKIP 30 LIMIT 15", query.findByType("Raptor", new Paging(2,15), 1).getStatement());
    }

    @Test
    public void testByProperty() {
        check("MATCH p=(n:`Raptor`)-[*0..2]-(m) WHERE n.`era` = { `era` } RETURN collect(distinct p) SKIP 100 LIMIT 10", query.findByProperty("Raptor", new Property<String, Object>("era", "Jurassic"), new Paging(10, 10), 2).getStatement());
    }

    @Test
    public void testFindByIdDepthZero() {
        check("MATCH (n) WHERE id(n) in { ids } RETURN collect(n) SKIP 1 LIMIT 1", query.findAll(Arrays.asList(23L, 24L), new Paging(1, 1), 0).getStatement());
    }

    @Test
    public void testFindByTypeDepthZero() {
        check("MATCH (n:`Raptor`) RETURN collect(n) SKIP 30 LIMIT 15", query.findByType("Raptor", new Paging(2,15), 0).getStatement());
    }

    @Test
    public void testByPropertyDepthZero() {
        check("MATCH (n:`Raptor`) WHERE n.`era` = { `era` } RETURN collect(n) SKIP 100 LIMIT 10", query.findByProperty("Raptor", new Property<String, Object>("era", "Jurassic"), new Paging(10, 10), 0).getStatement());
    }

    @Test
    public void testFindByIdDepthInfinite() {
        check("MATCH p=(n)-[*0..]-(m) WHERE id(n) in { ids } RETURN collect(distinct p) SKIP 1 LIMIT 1", query.findAll(Arrays.asList(23L, 24L), new Paging(1, 1), -1).getStatement());
    }

    @Test
    public void testFindByTypeDepthInfinite() {
        check("MATCH p=(n:`Raptor`)-[*0..]-(m) RETURN collect(distinct p) SKIP 30 LIMIT 15", query.findByType("Raptor", new Paging(2,15), -1).getStatement());
    }

    @Test
    public void testByPropertyDepthInfinite() {
        check("MATCH p=(n:`Raptor`)-[*0..]-(m) WHERE n.`era` = { `era` } RETURN collect(distinct p) SKIP 100 LIMIT 10", query.findByProperty("Raptor", new Property<String, Object>("era", "Jurassic"), new Paging(10, 10), -1).getStatement());
    }

    private void check(String expected, String actual) {
        assertEquals(expected, actual);
    }
}
