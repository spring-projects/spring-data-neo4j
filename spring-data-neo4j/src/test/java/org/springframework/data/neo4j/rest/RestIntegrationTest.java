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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.math.BigDecimal;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Verifies that Spring Data REST integration is present and correct.
 *
 * @author Adam George
 */
public class RestIntegrationTest {

    @ClassRule
    public static final RestIntegrationTestRule neo4jRule = new RestIntegrationTestRule();

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Before
    public void addTestDataIntoNeo4j() {
        neo4jRule.getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n, r");
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
        HttpResponse response = neo4jRule.sendGetRequest("/cricketers");
        assertEquals(200, response.getStatusLine().getStatusCode());

        JsonNode rootNode = this.jsonMapper.readTree(response.getEntity().getContent());
        JsonNode cricketers = rootNode.get("_embedded").get("cricketers");
        assertEquals(5, cricketers.size());

        response = neo4jRule.sendGetRequest("/teams");
        assertEquals(200, response.getStatusLine().getStatusCode());
        rootNode = this.jsonMapper.readTree(response.getEntity().getContent());
        JsonNode teams = rootNode.get("_embedded").get("teams");
        assertEquals(3, teams.size());
    }

    @Test
    public void shouldRetrieveParticularResourceByGraphId() throws IOException {
        HttpResponse response = neo4jRule.sendGetRequest("/teams");

        JsonNode rootNode = this.jsonMapper.readTree(response.getEntity().getContent());
        String teamUrl = rootNode.get("_embedded").get("teams").get(0).get("_links").get("self").get("href").asText();

        response = neo4jRule.sendGetRequest(teamUrl);
        assertEquals(200, response.getStatusLine().getStatusCode());
        JsonNode teamNode = this.jsonMapper.readTree(response.getEntity().getContent());
        assertNotNull("The team name should've been returned", teamNode.get("name"));
        assertEquals("The wrong team was returned", teamUrl, teamNode.get("_links").get("self").get("href").asText());
    }

    @Test
    public void shouldSupportAdditionAndRemovalOfNewResource() throws IOException {
        ObjectNode node = this.jsonMapper.createObjectNode();
        node.set("name", new TextNode("Abraham"));
        node.set("battingAverage", new DecimalNode(new BigDecimal("54.66")));

        HttpResponse response = neo4jRule.sendPostRequest(this.jsonMapper.writeValueAsBytes(node), "/cricketers");
        assertEquals(201, response.getStatusLine().getStatusCode());
        Header locationHeader = response.getFirstHeader("Location");
        assertNotNull("There should be a location header in the response", locationHeader);

        final String newResourceUrl = locationHeader.getValue();

        // verify addition
        response = neo4jRule.sendGetRequest(newResourceUrl);
        assertEquals(200, response.getStatusLine().getStatusCode());

        response = neo4jRule.sendDeleteRequest(newResourceUrl);
        assertEquals(204, response.getStatusLine().getStatusCode());

        // verify deletion
        response = neo4jRule.sendGetRequest(newResourceUrl);
        response.getEntity().writeTo(System.err);
        assertEquals(404, response.getStatusLine().getStatusCode());
    }

    /**
     * Strictly speaking, a PUT should replace a resource and set everything to null that isn't included in the payload
     * but the underlying OGM doesn't support setting <code>null</code> on properties.
     */
    @Test
    public void shouldSupportUpdateOfExistingResourceUsingHttpPut() throws IOException {
        HttpResponse response = neo4jRule.sendGetRequest("/cricketers");
        JsonNode rootNode = this.jsonMapper.readTree(response.getEntity().getContent());
        JsonNode cricketerNode = rootNode.get("_embedded").get("cricketers").get(2);
        String cricketerUrl = cricketerNode.get("_links").get("self").get("href").asText();

        response = neo4jRule.sendPutRequest("{\"name\":\"Shivnarine\"}".getBytes(), cricketerUrl);
        assertEquals(204, response.getStatusLine().getStatusCode());

        // verify update
        response = neo4jRule.sendGetRequest(cricketerUrl);
        assertEquals(200, response.getStatusLine().getStatusCode());
        JsonNode updatedCricketer = this.jsonMapper.readTree(response.getEntity().getContent());
        assertEquals("The name wasn't updated correctly", "Shivnarine", updatedCricketer.get("name").asText());
    }

    @Test
    public void shouldSupportPartialUpdateOfExistingResourceUsingHttpPatch() throws IOException {
        HttpResponse response = neo4jRule.sendGetRequest("/teams");
        JsonNode rootNode = this.jsonMapper.readTree(response.getEntity().getContent());
        JsonNode teamNode = rootNode.get("_embedded").get("teams").get(1);
        String teamUrl = teamNode.get("_links").get("self").get("href").asText();

        response = neo4jRule.sendPatchRequest("{\"name\":\"Lancashire Lightning\"}".getBytes(), teamUrl);
        assertEquals(204, response.getStatusLine().getStatusCode());

        // verify update
        response = neo4jRule.sendGetRequest(teamUrl);
        assertEquals(200, response.getStatusLine().getStatusCode());
        JsonNode updatedTeam = this.jsonMapper.readTree(response.getEntity().getContent());
        assertEquals("The name wasn't updated correctly", "Lancashire Lightning", updatedTeam.get("name").asText());
    }

    @Ignore
    @Test
    public void shouldAllowAdditionOfNewRelationshipsBetweenEntities() throws IOException {
        org.junit.Assert.fail("This test hasn't been written yet");
    }

    @Ignore
    @Test
    public void shouldAllowRemovalOfRelationshipBetweenEntities() throws IOException {
        org.junit.Assert.fail("This test hasn't been written yet");
    }

}
