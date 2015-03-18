/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.unit.mapper.cypher;

import org.neo4j.ogm.model.Property;
import org.junit.Test;
import org.neo4j.ogm.session.request.strategy.VariableDepthQuery;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

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
        assertEquals("MATCH p=(n:Orbit)-[*0..3]-(m) RETURN collect(distinct p)", query.findByType("Orbit", 3).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        assertEquals("MATCH p=(n:Asteroid)-[*0..4]-(m) WHERE n.diameter = { diameter } RETURN collect(distinct p)", query.findByProperty("Asteroid", new Property<String, Object>("diameter", 60.2), 4).getStatement());
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
        assertEquals("MATCH (n:Orbit) RETURN collect(n)", query.findByType("Orbit", 0).getStatement());
    }

    @Test
    public void testFindByPropertyZeroDepth() throws Exception {
        assertEquals("MATCH (n:Asteroid) WHERE n.diameter = { diameter } RETURN collect(n)", query.findByProperty("Asteroid", new Property<String, Object>("diameter", 60.2), 0).getStatement());
    }


}
