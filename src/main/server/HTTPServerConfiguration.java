package src.main.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HTTPServerConfiguration implements Configurable<HTTPServerConfiguration> {
    private List<HTTPListenerConfiguration> listeners = new ArrayList<>();
    private Path baseDir = Path.of("");
    private HTTPHandler httpHandler;
    private int maximumPendingSocketConnection = 500;
    private int keepAliveTimeout = 20;
    @Override
    public HTTPServerConfiguration configuration(){
        return this;
    }
    @Override
    public HTTPServerConfiguration withBaseDir(Path baseDir){
        this.baseDir = baseDir;
        return this;
    }
    @Override
    public HTTPServerConfiguration withListener(HTTPListenerConfiguration listener){
        this.listeners.add(listener);
        return this;
    }
    @Override
    public HTTPServerConfiguration withHandler(HTTPHandler handler){
        this.httpHandler = handler;
        return this;
    }
    public HTTPServerConfiguration withKeepAliveTimeout(int keepAliveTimeout){
        this.keepAliveTimeout = keepAliveTimeout;
        return this;
    }

    @Override
    public HTTPServerConfiguration withMaximumPendingSocketConnection(int maximumPendingSocketConnection){
        this.maximumPendingSocketConnection = maximumPendingSocketConnection;
        return this;
    }
    public int getKeepAliveTimeout(){
        return this.keepAliveTimeout;
    }
    public List<HTTPListenerConfiguration> getListeners(){
        return this.listeners;
    }

    public Path getBaseDir(){
        return this.baseDir;
    }
    public HTTPHandler getHandler(){
        return this.httpHandler;
    }
    public int getMaximumPendingSocketConnection(){
        return maximumPendingSocketConnection;
    }
}
