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

import org.junit.Test;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.request.strategy.VariableDepthQuery;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class VariableDepthQueryTest {

    private final VariableDepthQuery query = new VariableDepthQuery();

    @Test
    public void testFindOne() throws Exception {
        assertEquals("MATCH p=(n)-[*0..2]-(m) WHERE id(n) = { id } RETURN collect(distinct p)", query.findOne(0L, 2).getStatement());
    }

    @Test
    public void testFindAllCollection() throws Exception {
        assertEquals("MATCH p=(n)-[*0..1]-(m) WHERE id(n) in { ids } RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), 1).getStatement());
    }

    @Test
    public void testFindAll() throws Exception {
        assertEquals("MATCH p=()-->() RETURN p", query.findAll().getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        assertEquals("MATCH p=(n:`Orbit`)-[*0..3]-(m) RETURN collect(distinct p)", query.findByType("Orbit", 3).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        assertEquals("MATCH p=(n:`Asteroid`)-[*0..4]-(m) WHERE n.diameter = { diameter } RETURN collect(distinct p)", query.findByProperty("Asteroid", new Property<String, Object>("diameter", 60.2), 4).getStatement());
    }

    @Test
    public void testFindOneZeroDepth() throws Exception {
        assertEquals("MATCH (n) WHERE id(n) = { id } RETURN n", query.findOne(0L, 0).getStatement());
    }

    @Test
    public void testFindAllCollectionZeroDepth() throws Exception {
        assertEquals("MATCH (n) WHERE id(n) in { ids } RETURN collect(n)", query.findAll(Arrays.asList(1L, 2L, 3L), 0).getStatement());
    }

    @Test
    public void testFindByLabelZeroDepth() throws Exception {
        assertEquals("MATCH (n:`Orbit`) RETURN collect(n)", query.findByType("Orbit", 0).getStatement());
    }

    @Test
    public void testFindByPropertyZeroDepth() throws Exception {
        assertEquals("MATCH (n:`Asteroid`) WHERE n.diameter = { diameter } RETURN collect(n)", query.findByProperty("Asteroid", new Property<String, Object>("diameter", 60.2), 0).getStatement());
    }

    /**
     * @see DATAGRAPH-589
     * @throws Exception
     */
    @Test
    public void testFindByLabelWithIllegalCharacters() throws Exception {
        assertEquals("MATCH p=(n:`l'artiste`)-[*0..3]-(m) RETURN collect(distinct p)", query.findByType("l'artiste", 3).getStatement());
    }

    /**
     * @see DATAGRAPH-595
     * @throws Exception
     */
    @Test
    public void testFindOneNegativeDepth() throws Exception {
        assertEquals("MATCH p=(n)-[*0..]-(m) WHERE id(n) = { id } RETURN collect(distinct p)", query.findOne(0L, -1).getStatement());
    }

    /**
     * @see DATAGRAPH-595
     * @throws Exception
     */
    @Test
    public void testFindAllCollectionNegativeDepth() throws Exception {
        assertEquals("MATCH p=(n)-[*0..]-(m) WHERE id(n) in { ids } RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), -1).getStatement());
    }


    /**
     * @see DATAGRAPH-595
     * @throws Exception
     */
    @Test
    public void testFindByLabelNegativeDepth() throws Exception {
        assertEquals("MATCH p=(n:`Orbit`)-[*0..]-(m) RETURN collect(distinct p)", query.findByType("Orbit", -1).getStatement());
    }

    /**
     * @see DATAGRAPH-595
     * @throws Exception
     */
    @Test
    public void testFindByPropertyNegativeDepth() throws Exception {
        assertEquals("MATCH p=(n:`Asteroid`)-[*0..]-(m) WHERE n.diameter = { diameter } RETURN collect(distinct p)", query.findByProperty("Asteroid", new Property<String, Object>("diameter", 60.2), -1).getStatement());
    }



}
