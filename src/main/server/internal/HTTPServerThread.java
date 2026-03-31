package src.main.server.internal;

import src.main.server.HTTPListenerConfiguration;
import src.main.server.HTTPServerConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class HTTPServerThread extends Thread{
    private final HTTPServerConfiguration configuration;
    private final HTTPListenerConfiguration listener;
    private final ServerSocket serverSocket;
    private boolean running;

    public HTTPServerThread(HTTPServerConfiguration configuration, HTTPListenerConfiguration listener) throws IOException {
        this.configuration = configuration;
        this.listener = listener;
        this.serverSocket = new ServerSocket();

        serverSocket.setSoTimeout(0);
        serverSocket.bind(new InetSocketAddress(listener.getBindAddress(), listener.getPort()), configuration.getMaximumPendingSocketConnection());
    }

    @Override
    public void run(){

    }
}
