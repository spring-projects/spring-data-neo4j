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

import org.junit.Test;
import org.neo4j.ogm.session.request.strategy.DeleteStatements;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Vince Bickers
 */
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
