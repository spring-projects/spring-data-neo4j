/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest.support;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.configuration.Configuration;
import org.junit.*;
import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.server.NeoServer;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.server.configuration.ThirdPartyJaxRsPackage;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.springframework.data.neo4j.server.SpringPluginInitializer;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Path( "/" )
public class SpringPluginInitializerTests extends SpringPluginInitializer implements TestInterface {
    private NeoServer neoServer;

    public SpringPluginInitializerTests() {
        super( new String[]{"ServerTests-context.xml"}, expose("testObject", TestInterface.class) );
    }

    private static int touched = 0;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 7470;

    @Path( "/testInterface" )
    @POST
    @Produces( MediaType.APPLICATION_JSON )
    public void runThis( @Context TestInterface test ) {
        test.thisIsARecording();
    }

    @Path( "/testNoContext" )
    @POST
    @Produces( MediaType.APPLICATION_JSON )
    public void runThis() {
    }

    @Before
    public void setUp() throws Exception {
        ImpermanentGraphDatabase db = new ImpermanentGraphDatabase();
        final ServerConfigurator configurator = new ServerConfigurator(db) {
            @Override
            public List<ThirdPartyJaxRsPackage> getThirdpartyJaxRsPackages() {
                return Collections.singletonList(new ThirdPartyJaxRsPackage("org.springframework.data.neo4j.rest.support","/test"));
            }
        };
        final Configuration configuration = configurator.configuration();
        configuration.setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, PORT);
        configuration.setProperty("dbms.security.auth_enabled", false);
        final WrappingNeoServerBootstrapper bootstrapper = new WrappingNeoServerBootstrapper(db, configurator);
        touched=0;
        bootstrapper.start();
        neoServer = bootstrapper.getServer();
    }

    @After
    public void tearDown() throws Exception {
        neoServer.stop();
    }

    @Test
    public void shouldInjectInterface() throws Exception {
        RequestResult requestResult = sendRequest( "testInterface" );

        Assert.assertEquals( 204, requestResult.getStatus() );
        Assert.assertEquals( 1, touched );
    }

    @Test
    public void shouldWorkWithThirdPartyJaxrs() throws Exception {
        RequestResult requestResult = sendRequest( "testNoContext" );

        Assert.assertEquals( 204, requestResult.getStatus() );
    }

    private RequestResult sendRequest( String method ) {
        return RequestResult.extractFrom(Client.create().
                resource( "http://"+HOSTNAME+":"+PORT+"/test/" + method ).
                type( MediaType.APPLICATION_JSON ).
                accept( MediaType.APPLICATION_JSON ).post( ClientResponse.class ));
    }

    @Override
    public void thisIsARecording() {
        touched++;
    }
}
