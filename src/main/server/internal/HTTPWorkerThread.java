package src.main.server.internal;

import src.main.server.*;
import src.main.server.io.HTTPInputStream;
import src.main.server.io.HTTPOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;


public class HTTPWorkerThread implements Runnable {
    private final Socket socket;
    private final HTTPServerConfiguration config;
    private final HTTPListenerConfiguration listener;
    private volatile State state = State.Read;
    private final PushbackInputStream inputStream;
    public enum State { Read, Process, Write, KeepAlive }

    public HTTPWorkerThread(Socket socket, HTTPServerConfiguration config, HTTPListenerConfiguration listener) throws IOException{
        this.socket = socket;
        this.config = config;
        this.listener = listener;
        this.inputStream = new PushbackInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        // 1. Allocate Reusable Resources ONCE per connection
        byte[] headerBuffer = new byte[8192];
        HTTPInputStream httpInputStream;
        HTTPRequest request = null;
        HTTPResponse response = null;
        try {

            while (true) {
                state = State.Read;
                request = new HTTPRequest(listener.isTLS() ? "https" : "http" , listener.getPort(), socket.getInetAddress().getHostAddress());

                try {
                    // Parser fills the request object but does not wrap streams
                    HTTPParser.parse(inputStream, headerBuffer, request);
                } catch (IOException e) {
                    // Either the client closed the connection naturally, or sent bad headers.
                    break;
                }

                // 3. Worker orchestrates the streams
                long contentLength = parseContentLength(request);

                httpInputStream = new HTTPInputStream(inputStream, contentLength);
                request.setBodyStream(httpInputStream);

                
                response = new HTTPResponse();
                HTTPOutputStream hos = new HTTPOutputStream(socket.getOutputStream(), response);
                response.setOutputStream(hos);

                // 4. Pass control to your Business Logic (Handler)
                state = State.Process;
                try {
                    config.getHandler().handle(request, response);
                } catch (Exception e) {
                    // Catch unhandled errors in your web app to prevent crashing the worker
                    handleServerError(response);
                }

                // 5. Finalize the response (This guarantees headers/body are flushed)
                state = State.Write;
                response.close();

                // 6. Check Keep-Alive requirements
                if (!shouldKeepAlive(request, response)) {
                    break; // Exit loop, try-with-resources will close the socket
                }

                // 7. Prepare for the next request
                state = State.KeepAlive;
                socket.setSoTimeout(config.getKeepAliveTimeout());

                try {
                    // Drain up to 1MB of ignored body data. If it's more, kill the socket.
                    httpInputStream.drain(1024 * 1024);
                } catch (IOException e) {
                    System.err.println("Closing connection: " + e.getMessage());
                    break;
                }
            }
        } catch (SocketTimeoutException e) {
            // Keep-Alive timeout expired naturally. Not an error.
        } catch (IOException e) {
            System.err.println("Worker IO Error: " + e.getMessage());
        }
    }

    // --- Helper Methods ---

    private long parseContentLength(HTTPRequest request) {
        List<String> values = request.getHeaders("Content-Length");

        // Check if the list exists and has at least one value
        if (values == null || values.isEmpty()) {
            return 0;
        }

        String cl = values.get(0); // Safe to call now
        if (cl != null && !cl.isEmpty()) {
            try {
                return Long.parseLong(cl.trim());
            } catch (NumberFormatException ignored) {
                // Log this if you want to be strict
            }
        }
        return 0;
    }

    /**
     * Decides if the TCP socket should stay open for another request.
     */
    private boolean shouldKeepAlive(HTTPRequest request, HTTPResponse response) {

        String reqConn = request.getHeader("Connection", "");

        String resConn = response.getHeader("connection", "");


        if ("close".equalsIgnoreCase(resConn)) {
            return false;
        }


        if ("HTTP/1.1".equals(request.getVersion())) {
            return !"close".equalsIgnoreCase(reqConn);
        }

        return "keep-alive".equalsIgnoreCase(reqConn);
    }

    /**
     * Failsafe for 500 Internal Server Errors.
     */
    private void handleServerError(HTTPResponse response) {
        try {
            if (!response.isCommitted()) {
                response.reset();
                response.setStatus(500, "Internal Server Error");
                response.setHeader("Connection", "close");
                response.getWriter().write("500 - An unexpected error occurred.");
            }
        } catch (IOException ignored) {}
    }
}