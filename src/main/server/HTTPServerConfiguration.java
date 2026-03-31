package src.main.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HTTPServerConfiguration implements Configurable<HTTPServerConfiguration> {
    private List<HTTPListenerConfiguration> listeners = new ArrayList<>();
    private Path baseDir = Path.of("");
    private HTTPHandler httpHandler;
    private int maximumPendingSocketConnection;
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
    @Override
    public HTTPServerConfiguration withMaximumPendingSocketConnection(int maximumPendingSocketConnection){
        this.maximumPendingSocketConnection = maximumPendingSocketConnection;
        return this;
    }

    public List<HTTPListenerConfiguration> getListeners(){
        return this.listeners;
    }

    public Path getBaseDir(){
        return this.baseDir;
    }
    public HTTPHandler getHttpHandler(){
        return this.httpHandler;
    }
    public int getMaximumPendingSocketConnection(){
        return maximumPendingSocketConnection;
    }
}
