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
import org.neo4j.ogm.session.request.strategy.DeleteNodeStatements;
import org.neo4j.ogm.session.request.strategy.DeleteRelationshipStatements;
import org.neo4j.ogm.session.request.strategy.DeleteStatements;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class DeleteStatementsTest {

    private final DeleteStatements deleteNodeStatements = new DeleteNodeStatements();
    private final DeleteStatements deleteRelStatements = new DeleteRelationshipStatements();

    @Test
    public void testDeleteOneNode() {
        assertEquals("MATCH (n) WHERE id(n) = { id } OPTIONAL MATCH (n)-[r]-() DELETE r, n", deleteNodeStatements.delete(0L).getStatement());
    }

    @Test
    public void testDeleteAllNodes() {
        assertEquals("MATCH (n) WHERE id(n) in { ids } OPTIONAL MATCH (n)-[r]-() DELETE r, n", deleteNodeStatements.deleteAll(Arrays.asList(1L, 2L)).getStatement());
    }

    @Test
    public void testPurge() {
        assertEquals("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n", deleteNodeStatements.purge().getStatement());
    }

    @Test
    public void testDeleteByLabel() {
        assertEquals("MATCH (n:`TRAFFIC_WARDENS`) OPTIONAL MATCH (n)-[r]-() DELETE r, n", deleteNodeStatements.deleteByType("TRAFFIC_WARDENS").getStatement());
    }

    @Test
    public void testDeleteOneRel() {
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) = { id } DELETE r", deleteRelStatements.delete(0L).getStatement());
    }

    @Test
    public void testDeleteAllRels() {
        assertEquals("MATCH (n)-[r]->() WHERE id(r) in { ids } DELETE r", deleteRelStatements.deleteAll(Arrays.asList(1L, 2L)).getStatement());
    }


    @Test
    public void testDeleteByType() {
        assertEquals("MATCH (n)-[r:`TRAFFIC_WARDEN`]-() DELETE r", deleteRelStatements.deleteByType("TRAFFIC_WARDEN").getStatement());
    }
}
