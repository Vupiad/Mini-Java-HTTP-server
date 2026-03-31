package src.main.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class HTTPListenerConfiguration {
    private final InetAddress bindAddress;
    private final int port;
//    private final Certificate[] certificatesChain;
//    private final PrivateKey privateKey;
//    private final boolean tls;

    public HTTPListenerConfiguration(InetAddress bindAddress, int port) {
        this.bindAddress = bindAddress;
        this.port = port;
    }

    // Convenience constructor: Defaults to "0.0.0.0" (All interfaces)
    public HTTPListenerConfiguration(int port) {
        this.port = port;
        try {
            // "0.0.0.0" allows the server to be reached from other devices on the network
            this.bindAddress = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            // This should technically never happen for 0.0.0.0
            throw new RuntimeException("Failed to initialize default bind address", e);
        }
    }

    public InetAddress getBindAddress() {
        return bindAddress;
    }

    public int getPort() {
        return port;
    }



}
