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

package org.neo4j.ogm.integration;

import org.junit.ClassRule;
import org.junit.experimental.categories.Category;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;

/**
 * Base class to facilitate testing a {@link Session} against an in-memory Neo4j database.
 *
 * @deprecated Use {@link Neo4jIntegrationTestRule} directly instead
 * @author Michal Bachman
 */
@Deprecated
@Category(IntegrationTest.class)
//TODO: since we're not @RunningWith(Categories.class) anywhere, do we need this class at all?
public abstract class InMemoryServerTest {

    @ClassRule
    public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule();

    protected static Session session;

    protected static void load(String cqlFile) {
        neo4jRule.loadClasspathCypherScriptFile(cqlFile);
    }

    protected static String baseNeoUrl() {
        return neo4jRule.baseNeoUrl();
    }

}
