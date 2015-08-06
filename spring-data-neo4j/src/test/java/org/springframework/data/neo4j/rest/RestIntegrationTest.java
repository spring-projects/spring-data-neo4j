/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.rest;

import static org.junit.Assert.*;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Verifies that Spring Data REST integration is present and correct.
 *
 * @author Adam George
 */
@Ignore
public class RestIntegrationTest {

    @ClassRule
    public static final RestIntegrationTestRule neo4jRule = new RestIntegrationTestRule();

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @BeforeClass
    public static void addTestDataIntoNeo4j() {
        neo4jRule.getGraphDatabaseService().execute("CREATE (v:Cricketer:Batsman {name:'Virat',battingAverage:'50.11'}), "
                + "(b:Cricketer:Batsman:WicketKeeper {name:'Brendon',battingAverage:'38.91'}), "
                + "(s:Cricketer:Batsman:Bowler {name:'Shane',battingAverage:'42.79'}), "
                + "(w:Cricketer:Bowler {name:'Wahab'}), "
                + "(a:Cricketer:Batsman {name:'Alastair',battingAverage:'45.20'}), "
                + "(essex:Team {name:'Essex'}), (lancs:Team {name:'Lancashire'}), (scum:Team {name:'Yorkshire'}) "
                + "WITH a, essex MERGE (a)-[:PLAYS_FOR]->(essex)");
    }

    @Test
    public void shouldRetrieveAllResourcesOfParticularType() throws IOException {
        HttpResponse response = neo4jRule.sendRequest("/cricketers");
        assertEquals(200, response.getStatusLine().getStatusCode());

        JsonNode rootNode = this.jsonMapper.readTree(response.getEntity().getContent());
        JsonNode cricketers = rootNode.get("_embedded").get("cricketers");
        assertEquals(5, cricketers.size());

        response = neo4jRule.sendRequest("/teams");
        assertEquals(200, response.getStatusLine().getStatusCode());
        rootNode = this.jsonMapper.readTree(response.getEntity().getContent());
        JsonNode teams = rootNode.get("_embedded").get("teams");
        assertEquals(3, teams.size());
    }

    @Ignore
    @Test
    public void shouldRetrieveParticularResourceByGraphId() {
        org.junit.Assert.fail("This test hasn't been written yet");
    }

    @Ignore
    @Test
    public void shouldSupportAdditionAndRemovalOfNewResource() {
        org.junit.Assert.fail("This test hasn't been written yet");
    }

    @Ignore
    @Test
    public void shouldSupportUpdateOfExistingResourceUsingHttpPut() {
        org.junit.Assert.fail("This test hasn't been written yet");
    }

    @Ignore
    @Test
    public void shouldSupportUpdateOfExistingResourceUsingHttpPatch() {
        org.junit.Assert.fail("This test hasn't been written yet");
    }

    @Ignore
    @Test
    public void shouldAllowAdditionOfNewRelationshipsBetweenEntities() {
        org.junit.Assert.fail("This test hasn't been written yet");
    }

    @Ignore
    @Test
    public void shouldAllowRemovalOfRelationshipBetweenEntities() {
        org.junit.Assert.fail("This test hasn't been written yet");
    }

}
