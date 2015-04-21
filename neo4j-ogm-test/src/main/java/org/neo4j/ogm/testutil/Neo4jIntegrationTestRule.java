/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.neo4j.ogm.testutil;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * JUnit {@link TestRule} that provides a {@link GraphDatabaseService} to its enclosing test harness via both the object itself
 * and a remote server running on a local port.  The port is configurable via this rule's constructor.
 * <p>
 * You can use this as a normal rule or as a class-level rule depending on your needs. Class-level use means you'll get one
 * database for the whole test class and is therefore normally faster.
 * <pre>
 * public class MyJUnitTest {
 *
 *     &#064;ClassRule
 *     public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule();
 *
 * }
 * </pre>
 * If you need a clean database for each individual test method then you can simply include this as a non-static rule instead:
 * <pre>
 * public class MyOtherJUnitTest {
 *
 *     &#064;Rule
 *     public Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule();
 *
 * }
 * </pre>
 * You can call methods on this rule from within your test methods to facilitate writing integration tests.
 * </p>
 *
 * @author Adam George
 */
@SuppressWarnings("deprecation")
public class Neo4jIntegrationTestRule implements TestRule {

    private static final int DEFAULT_TEST_SERVER_PORT = 7575;

    private final int port;
    private WrappingNeoServerBootstrapper bootstrapper;
    private GraphDatabaseService database;

    /**
     * Constructs a new {@link Neo4jIntegrationTestRule} that starts a Neo4j server listening on any available local port.
     */
    public Neo4jIntegrationTestRule() {
        this(findAvailableLocalPort());
    }

    /**
     * Constructs a new {@link Neo4jIntegrationTestRule} that starts a Neo4j server listening on the specified port.
     *
     * @param neoServerPort The local TCP port on which the Neo4j database server should run
     */
    public Neo4jIntegrationTestRule(int neoServerPort) {
        this.port = neoServerPort;
        this.database = new TestGraphDatabaseFactory().newImpermanentDatabase();
        this.bootstrapper = createServerWrapper();
    }

    private static int findAvailableLocalPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            System.err.println("Unable to establish local port for Neo4j test server due to IOException: " + e.getMessage()
                    + "\nDefaulting instead to use: " + DEFAULT_TEST_SERVER_PORT);
            e.printStackTrace(System.err);

            return DEFAULT_TEST_SERVER_PORT;
        }
    }

    private WrappingNeoServerBootstrapper createServerWrapper() {
        ServerConfigurator configurator = new ServerConfigurator((GraphDatabaseAPI) this.database);
        configurator.configuration().addProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, this.port);
        return new WrappingNeoServerBootstrapper((GraphDatabaseAPI) this.database, configurator);
    }

    @Override
    public Statement apply(final Statement baseStatement, Description description) {
        this.bootstrapper.start();

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    if (database.isAvailable(1000)) {
                        baseStatement.evaluate();
                    } else {
                        Assert.fail("Database was shut down or didn't become available within 1s");
                    }
                } finally {
                    stopBootstrapper();
                }
            }
        };
    }

    /**
     * Stops the underlying server bootstrapper and, in turn, the Neo4j server.
     */
    public void stopBootstrapper() {
        this.bootstrapper.stop();
    }

    /**
     * Retrieves the base URL of the Neo4j database server used in the test.
     *
     * @return The URL of the Neo4j test server
     */
    public String baseNeoUrl() {
        return "http://localhost:" + this.port;
    }

    /**
     * Loads the specified CQL file from the classpath into the database.
     *
     * @param cqlFileName The name of the CQL file to load
     */
    public void loadClasspathCypherScriptFile(String cqlFileName) {
        StringBuilder cypher = new StringBuilder();
        try (Scanner scanner = new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream(cqlFileName))) {
            scanner.useDelimiter(System.getProperty("line.separator"));
            while (scanner.hasNext()) {
                cypher.append(scanner.next()).append(' ');
            }
        }

        new ExecutionEngine(this.database).execute(cypher.toString());
    }

    /**
     * Deletes all the nodes and relationships in the test database.
     */
    public void clearDatabase() {
        new ExecutionEngine(this.database).execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
    }

    /**
     * Retrieves the underlying {@link GraphDatabaseService} used in this test.
     *
     * @return The test {@link GraphDatabaseService}
     */
    public GraphDatabaseService getGraphDatabaseService() {
        return this.database;
    }

}
