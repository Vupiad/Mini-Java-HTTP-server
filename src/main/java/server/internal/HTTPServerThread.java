package src.main.java.server.internal;

import src.main.java.server.HTTPListenerConfiguration;
import src.main.java.server.HTTPServerConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class HTTPServerThread extends Thread {
    private final HTTPServerConfiguration configuration;
    private final HTTPListenerConfiguration listener;
    private final ServerSocket serverSocket;
    private volatile boolean running = true;

    public HTTPServerThread(HTTPServerConfiguration configuration, HTTPListenerConfiguration listener) throws IOException {
        super("http-server-listener-" + listener.getPort());
        this.configuration = configuration;
        this.listener = listener;
        this.serverSocket = new ServerSocket();

        // 0 means infinite wait for accept()
        serverSocket.setSoTimeout(0);
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(listener.getBindAddress(), listener.getPort()),
                configuration.getMaximumPendingSocketConnection());
    }

    @Override
    public void run() {
        System.out.println("Server started on " + listener.getBindAddress() + ":" + listener.getPort());

        while (running) {
            try {
                // 1. Block and wait for a client (Browser) to connect
                Socket clientSocket = serverSocket.accept();

                // 2. Configure the socket (Optional: Disable Nagle's algorithm for faster small responses)
                clientSocket.setTcpNoDelay(true);

                // 3. Hand off to a Worker using a Virtual Thread
                // This is where the magic happens - it's non-blocking for this thread!
                HTTPWorkerThread worker = new HTTPWorkerThread(clientSocket, configuration, listener);
                Thread.ofVirtual()
                        .name("http-worker-", 0)
                        .start(worker);

            } catch (SocketException e) {
                if (!running) {
                    break; // Server is shutting down
                }
                System.err.println("Socket error: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Failed to accept connection: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        this.running = false;
        try {
            serverSocket.close(); // This breaks the blocking accept() call
        } catch (IOException e) {
            // Ignore
        }
    }
}