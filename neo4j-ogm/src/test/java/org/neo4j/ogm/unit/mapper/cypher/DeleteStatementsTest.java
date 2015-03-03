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

import org.junit.Test;
import org.neo4j.ogm.session.request.strategy.DeleteStatements;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class DeleteStatementsTest {

    private final DeleteStatements statements = new DeleteStatements();

    @Test
    public void testDeleteOne() {
        assertEquals("MATCH (n) WHERE id(n) = { id } OPTIONAL MATCH (n)-[r]-() DELETE r, n", statements.delete(0L).getStatement());
    }

    @Test
    public void testDeleteAll() {
        assertEquals("MATCH (n) WHERE id(n) in { ids } OPTIONAL MATCH (n)-[r]-() DELETE r, n", statements.deleteAll(Arrays.asList(1L, 2L)).getStatement());
    }

    @Test
    public void testPurge() {
        assertEquals("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n", statements.purge().getStatement());
    }

    @Test
    public void testDeleteByLabel() {
        assertEquals("MATCH (n:TRAFFIC_WARDENS) OPTIONAL MATCH (n)-[r]-() DELETE r, n", statements.deleteByLabel("TRAFFIC_WARDENS").getStatement());
    }
}
