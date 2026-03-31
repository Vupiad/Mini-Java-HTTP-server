package src.main.server;

import java.nio.file.Path;

public interface Configurable<T extends Configurable<T>> {
    HTTPServerConfiguration configuration();

    default T withBaseDir(Path baseDir){
        configuration().withBaseDir(baseDir);
        return (T) this;
    }

    default T withHandler(HTTPHandler handler){
        configuration().withHandler(handler);
        return (T) this;
    }

    default T withListener(HTTPListenerConfiguration listener){
        configuration().withListener(listener);
        return (T) this;
    }

    default T withMaximumPendingSocketConnection(int maximumPendingSocketConnection){
        configuration().withMaximumPendingSocketConnection(maximumPendingSocketConnection);
        return (T) this;
    }
}
