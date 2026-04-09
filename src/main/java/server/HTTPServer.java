package src.main.java.server;

import src.main.java.server.internal.HTTPServerThread;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HTTPServer implements Closeable, Configurable<HTTPServer> {
    private final List<HTTPServerThread> servers = new ArrayList<>();
    private final HTTPServerConfiguration configuration = new HTTPServerConfiguration();

    /**
     * Starts the server by spawning listener threads for every configured listener.
     */
    public HTTPServer start() {
        System.out.println("Start the servers");
        for (HTTPListenerConfiguration listener : configuration.getListeners()) {
            try {
                System.out.println("Start server thread");
                HTTPServerThread server = new HTTPServerThread(configuration, listener);
                server.run();
                servers.add(server);
            } catch (IOException e) {
                System.err.println("Could not start listener on port " + listener.getPort() + ": " + e.getMessage());
                // In a real system, you might want to call close() here to roll back if one fails
            }
        }
        return this;
    }

    /**
     * Gracefully stops all listener threads.
     */
    @Override
    public void close() {
        System.out.println("Shutting down HTTP server...");
        for (HTTPServerThread server : servers) {
            server.shutdown();
        }
        servers.clear();
    }

    @Override
    public HTTPServerConfiguration configuration() {
        return this.configuration;
    }
}