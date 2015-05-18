package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.Test;
import org.neo4j.ogm.cypher.query.Paging;
import org.neo4j.ogm.model.Property;
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
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) IN { ids } WITH n MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p) SKIP 30 LIMIT 10", query.findAll(Arrays.asList(1L, 2L, 3L), new Paging(3, 10), 1).getStatement());
    }

    @Test
    public void testFindAll() throws Exception {
        assertEquals("MATCH p=()-->() RETURN p SKIP 2 LIMIT 2", query.findAll(new Paging(1, 2)).getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        assertEquals("MATCH p=(n)-[:`EATS`]-(m) RETURN collect(distinct p) SKIP 2 LIMIT 2", query.findByType("ORBITS", new Paging(1, 10), 3).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        assertEquals("MATCH (n)-[r:`ORBITS`]->() WHERE r.`distance` = { `distance` } WITH n MATCH p=(n)-[*0..4]-(m) RETURN collect(distinct p) SKIP 150 LIMIT 15", query.findByProperty("ORBITS", new Property<String, Object>("distance", 60.2), new Paging(10, 15), 4).getStatement());
    }

}
