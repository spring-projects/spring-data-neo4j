/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */
package org.springframework.data.neo4j.auth;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.harness.internal.InProcessServerControls;
import org.neo4j.kernel.Version;
import org.neo4j.ogm.domain.bike.Bike;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.result.ResultProcessingException;
import org.neo4j.ogm.session.transaction.Transaction;
import org.neo4j.ogm.testutil.TestUtils;
import org.neo4j.server.AbstractNeoServer;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;
import org.springframework.data.neo4j.template.Neo4jOperations;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class AuthenticationTest
{
    private static AbstractNeoServer neoServer;
    private static int neoPort;
    private Session session;
    private Neo4jOperations template;

    private boolean AUTH = true;
    private boolean NO_AUTH = false;

    @BeforeClass
    public static void setUp() throws Exception {

        Path authStore = Files.createTempFile( "neo4j", "credentials" );
        authStore.toFile().deleteOnExit();
        try (Writer authStoreWriter = new FileWriter( authStore.toFile() )) {
            IOUtils.write( "neo4j:SHA-256,03C9C54BF6EEF1FF3DFEB75403401AA0EBA97860CAC187D6452A1FCF4C63353A,819BDB957119F8DFFF65604C92980A91:", authStoreWriter );
        }

        neoPort = TestUtils.getAvailablePort();

        try {
            ServerControls controls = TestServerBuilders.newInProcessBuilder()
                    .withConfig("dbms.security.auth_enabled", "true")
                    .withConfig("org.neo4j.server.webserver.port", String.valueOf(neoPort))
                    .withConfig("dbms.security.auth_store.location", authStore.toAbsolutePath().toString())
                    .newServer();

            initialise(controls);

        } catch (Exception e) {
            throw new RuntimeException("Error starting in-process server",e);
        }
    }

    private static void initialise(ServerControls controls) throws Exception {

        Field field = InProcessServerControls.class.getDeclaredField("server");
        field.setAccessible(true);
        neoServer = (AbstractNeoServer) field.get(controls);
    }

    @Test
    public void testUnauthorizedSession() throws Exception {
        assumeTrue(isRunningWithNeo4j2Dot2OrLater());

        init( NO_AUTH, "org.neo4j.ogm.domain.bike" );

        try ( Transaction tx = session.beginTransaction() ) {
            template.loadAll(Bike.class);
            fail("A non-authenticating version of Neo4j is running. Please start Neo4j 2.2.0 or later to run these tests");
        } catch (ResultProcessingException rpe) {
            Throwable cause = rpe.getCause();
            if (cause instanceof HttpHostConnectException) {
                fail("Please start Neo4j 2.2.0 or later to run these tests");
            } else {
                assertTrue(cause instanceof HttpResponseException);
                assertEquals("Unauthorized", cause.getMessage());
            }
        }

    }

    // good enough for now: ignore test if we are not on something better than 2.1
    private boolean isRunningWithNeo4j2Dot2OrLater() {
        BigDecimal version = new BigDecimal(Version.getKernelRevision().substring(0,3));
        return version.compareTo(new BigDecimal("2.1")) > 0;
    }

    @Test
    public void testAuthorizedSession() throws Exception {
        assumeTrue(isRunningWithNeo4j2Dot2OrLater());

        init(AUTH, "org.neo4j.ogm.domain.bike");

        try ( Transaction ignored = session.beginTransaction()) {
            template.loadAll(Bike.class);
        } catch (ResultProcessingException rpe) {
            fail("'" + rpe.getCause().getLocalizedMessage() + "' was not expected here");
        }

    }

    /**
     * @see DATAGRAPH-745
     * @throws Exception
     */
    @Test
    public void testAuthorizedSessionWithSuppliedCredentials() throws Exception {
        assumeTrue(isRunningWithNeo4j2Dot2OrLater());

        initWithSuppliedCredentials("neo4j", "password", "org.neo4j.ogm.domain.bike");

        try ( Transaction ignored = session.beginTransaction() ) {
            template.loadAll(Bike.class);
        } catch (ResultProcessingException rpe) {
            fail("'" + rpe.getCause().getLocalizedMessage() + "' was not expected here");
        }

    }

    /**
     * @see DATAGRAPH-745
     * @throws Exception
     */
    @Test
    public void testUnauthorizedSessionWithSuppliedCredentials() throws Exception {
        assumeTrue(isRunningWithNeo4j2Dot2OrLater());

        initWithSuppliedCredentials("neo4j", "incorrectPassword", "org.neo4j.ogm.domain.bike");

        try ( Transaction tx = session.beginTransaction() ) {
            template.loadAll(Bike.class);
            fail("A non-authenticating version of Neo4j is running. Please start Neo4j 2.2.0 or later to run these tests");
        } catch (ResultProcessingException rpe) {
            Throwable cause = rpe.getCause();
            if (cause instanceof HttpHostConnectException) {
                fail("Please start Neo4j 2.2.0 or later to run these tests");
            } else {
                assertTrue(cause instanceof HttpResponseException);
                assertEquals("Unauthorized", cause.getMessage());
            }
        }

    }

    /**
     * @see DATAGRAPH-745
     * @throws Exception
     */
    @Test
    public void testAuthorizedSessionWithURI() throws Exception {
        assumeTrue(isRunningWithNeo4j2Dot2OrLater());

        URI neoUri = neoServer.baseUri();
        initWithEmbeddedCredentials("http://neo4j:password@" + neoUri.getHost() + ":" + neoUri.getPort(), "org.neo4j.ogm.domain.bike");

        try ( Transaction ignored = session.beginTransaction() ) {
            template.loadAll(Bike.class);
        } catch (ResultProcessingException rpe) {
            fail("'" + rpe.getCause().getLocalizedMessage() + "' was not expected here");
        }

    }

    /**
     * @see DATAGRAPH-745
     * @throws Exception
     */
    @Test
    public void testUnauthorizedSessionWithURI() throws Exception {
        assumeTrue(isRunningWithNeo4j2Dot2OrLater());

        initWithEmbeddedCredentials(neoServer.baseUri().toString(), "org.neo4j.ogm.domain.bike");

        try ( Transaction tx = session.beginTransaction() ) {
            template.loadAll(Bike.class);
            fail("A non-authenticating version of Neo4j is running. Please start Neo4j 2.2.0 or later to run these tests");
        } catch (ResultProcessingException rpe) {
            Throwable cause = rpe.getCause();
            if (cause instanceof HttpHostConnectException) {
                fail("Please start Neo4j 2.2.0 or later to run these tests");
            } else {
                assertTrue(cause instanceof HttpResponseException);
                assertEquals("Unauthorized", cause.getMessage());
            }
        }

    }

    private void init(boolean auth, final String... packages) throws Exception {

        if (auth) {
            System.setProperty("username", "neo4j");
            System.setProperty("password", "password");
        } else {
            System.getProperties().remove("username");
            System.getProperties().remove("password");
        }

        Neo4jConfiguration configuration = new Neo4jConfiguration() {
            @Override
            public Neo4jServer neo4jServer() {
                return new RemoteServer(neoServer.baseUri().toString());
            }

            @Override
            public SessionFactory getSessionFactory() {
                return new SessionFactory(packages);
            }
        };
        template = configuration.neo4jTemplate();
        session = configuration.getSession();


    }

    private void initWithSuppliedCredentials(final String username, final String password, final String... packages) throws Exception {
        System.getProperties().remove("username");
        System.getProperties().remove("password");

        Neo4jConfiguration configuration = new Neo4jConfiguration() {
            @Override
            public Neo4jServer neo4jServer() {
                return new RemoteServer(neoServer.baseUri().toString(), username, password);
            }

            @Override
            public SessionFactory getSessionFactory() {
                return new SessionFactory(packages);
            }
        };
        template = configuration.neo4jTemplate();
        session = configuration.getSession();
    }

    private void initWithEmbeddedCredentials(final String url, final String... packages) throws Exception {
        System.getProperties().remove("username");
        System.getProperties().remove("password");

        Neo4jConfiguration configuration = new Neo4jConfiguration() {
            @Override
            public Neo4jServer neo4jServer() {
                return new RemoteServer(url);
            }

            @Override
            public SessionFactory getSessionFactory() {
                return new SessionFactory(packages);
            }
        };
        template = configuration.neo4jTemplate();
        session = configuration.getSession();
    }
}
