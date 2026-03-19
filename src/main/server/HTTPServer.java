package src.main.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HTTPServer {
    private int port;

    public HTTPServer(int port) {
        this.port = port;
    }

    public void start() {
        // We use try-with-resources for the ServerSocket too
        try (ServerSocket server = new ServerSocket(this.port);
             ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("Server started at port " + this.port);

            while (true) {
                // Accept is a blocking call; it waits here for a client
                try (Socket clientSocket = server.accept()) {
                    HTTPWorkerThread workerThread = new HTTPWorkerThread(clientSocket);
                    executorService.submit(workerThread);

                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + this.port + " - " + e.getMessage());
        }
    }
}