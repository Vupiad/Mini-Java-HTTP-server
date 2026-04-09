package src.main.java.server;

@FunctionalInterface
public interface HTTPHandler {
    void handle(HTTPRequest request, HTTPResponse response) throws Exception;
}
