package org.springframework.data.neo4j.server;

public class RemoteServer implements Neo4jServer {

    private final String url;

    public RemoteServer(String url) {
        this.url = url;
    }

    public String url() {
        return this.url;
    }

}