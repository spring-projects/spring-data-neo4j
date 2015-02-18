package org.springframework.data.neo4j.server;

import java.io.IOException;
import java.net.ServerSocket;

final class TestUtils {

    public static int getAvailablePort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            try {
                return socket.getLocalPort();
            } finally {
                socket.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find available port: " + e.getMessage(), e);
        }
    }

    private TestUtils() {
    }
}
