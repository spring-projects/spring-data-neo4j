package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.Test;
import org.neo4j.ogm.exception.InvalidDepthException;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.request.strategy.QueryStatements;
import org.neo4j.ogm.session.request.strategy.VariableDepthRelationshipQuery;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author: Vince Bickers
 */
public class RelationshipEntityQueryTest {
    
    private final QueryStatements query = new VariableDepthRelationshipQuery();

    @Test
    public void testFindOne() throws Exception {
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) = { id } WITH n MATCH p=(n)-[*0..2]-(m) RETURN collect(distinct p)", query.findOne(0L, 2).getStatement());
    }

    @Test
    public void testFindAllCollection() throws Exception {
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) IN { ids } WITH n MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), 1).getStatement());
    }

    @Test
    public void testFindAll() throws Exception {
        assertEquals("MATCH p=()-->() RETURN p", query.findAll().getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        assertEquals("MATCH p=(n)-[:`ORBITS`*..3]-(m) RETURN collect(distinct p)", query.findByType("ORBITS", 3).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        assertEquals("MATCH (n)-[r:`ORBITS`]->() WHERE r.`distance` = { `distance` } WITH n MATCH p=(n)-[*0..4]-(m) RETURN collect(distinct p)", query.findByProperty("ORBITS", new Property<String, Object>("distance", 60.2), 4).getStatement());
    }

    @Test(expected = InvalidDepthException.class)
    public void testFindOneZeroDepth() throws Exception {
        query.findOne(0L, 0).getStatement();
    }

    @Test(expected = InvalidDepthException.class)
    public void testFindAllCollectionZeroDepth() throws Exception {
        query.findAll(Arrays.asList(1L, 2L, 3L), 0).getStatement();
    }

    @Test(expected = InvalidDepthException.class)
    public void testFindByLabelZeroDepth() throws Exception {
        query.findByType("ORBITS", 0).getStatement();
    }

    @Test(expected = InvalidDepthException.class)
    public void testFindByPropertyZeroDepth() throws Exception {
        query.findByProperty("ORBITS", new Property<String, Object>("perihelion", 19.7), 0).getStatement();
    }

    @Test(expected = InvalidDepthException.class)
    public void testFindOneInfiniteDepth() throws Exception {
        query.findOne(0L, -1).getStatement();
    }

    @Test(expected = InvalidDepthException.class)
    public void testFindAllCollectionInfiniteDepth() throws Exception {
        query.findAll(Arrays.asList(1L, 2L, 3L), -1).getStatement();
    }

    @Test(expected = InvalidDepthException.class)
    public void testFindByLabelInfiniteDepth() throws Exception {
        query.findByType("ORBITS", -1).getStatement();
    }

    @Test(expected = InvalidDepthException.class)
    public void testFindByPropertyInfiniteDepth() throws Exception {
        query.findByProperty("ORBITS", new Property<String, Object>("period", 2103.776), -1).getStatement();
    }

}
