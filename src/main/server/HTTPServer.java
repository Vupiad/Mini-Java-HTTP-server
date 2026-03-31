package src.main.server;


import src.main.server.internal.HTTPServerThread;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HTTPServer implements Closeable, Configurable<HTTPServer> {
    List<HTTPServerThread> servers = new ArrayList<>();
    HTTPServerConfiguration configuration = new HTTPServerConfiguration();


    @Override
    public void close() throws IOException {

    }
    public HTTPServer start(){
        return this;
    }
    @Override
    public HTTPServerConfiguration configuration() {
        return this.configuration;
    }


}