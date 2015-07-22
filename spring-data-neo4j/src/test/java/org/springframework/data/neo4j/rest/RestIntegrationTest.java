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

import java.io.IOException;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Verifies that Spring Data REST integration is present and correct.
 *
 * @author Adam George
 */
public class RestIntegrationTest {

    @ClassRule
    public static final RestIntegrationTestRule neo4jRule = new RestIntegrationTestRule();

    @BeforeClass
    public static void addTestDataIntoNeo4j() {
        neo4jRule.getGraphDatabaseService().execute("CREATE (v:Cricketer:Batsman {name:'Virat',battingAverage:'50.11'}), "
                + "(b:Cricketer:Batsman:WicketKeeper {name:'Brendon',battingAverage:'38.91'}), "
                + "(s:Cricketer:Batsman:Bowler {name:'Shane',battingAverage:'42.79'}), "
                + "(w:Cricketer:Bowler {name:'Wahab'}), "
                + "(a:Cricketer:Batsman {name:'Alastair',battingAverage:'45.20'})");
    }

    @Test
    public void shouldRetrieveAllResourcesOfParticularType() {
        HttpClient client = HttpClientBuilder.create()
                .setDefaultHeaders(Arrays.asList(new BasicHeader("Content-Type", "application/hal+json"))).build();

        try {
            HttpResponse response = client.execute(new HttpGet(neo4jRule.getRestBaseUrl() + "/cricketers"));
//            response.getEntity().writeTo(System.out);
            assertEquals(200, response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            e.printStackTrace(System.err);
            org.junit.Assert.fail(e.getMessage());
        }
    }

}
