package org.springframework.data.neo4j.server;

import org.neo4j.server.NeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;

import java.io.IOException;

public class InProcessServer implements Neo4jServer {

    private final NeoServer neoServer;
    protected int neoPort;

    public InProcessServer()  {
        neoPort = TestUtils.getAvailablePort();
        try {
            neoServer = CommunityServerBuilder.server().onPort(neoPort).build();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    neoServer.stop();
                }
            });
            neoServer.start();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public String url() {
        return neoServer.baseUri().toString();
    }


}