package src.main.server;

import java.io.InputStream;
import java.util.Map;
import src.main.server.utils.CaseInsensitiveMap;

public class HTTPRequest {
    private final String method;
    private final String path;
    private String version;

    private InputStream bodyStream;// The "Lazy" handle
    private final Map<String, String> headers = new CaseInsensitiveMap();

    public HTTPRequest(String method, String path, Map<String, String> headers, InputStream bodyStream) {
        this.method = method;
        this.path = path;
        this.bodyStream = bodyStream;
    }

    public HTTPRequest(String method, String path, String version){
        this.method = method;
        this.path = path;
        this.version = version;
    }



    // The Handler calls this only if it needs the data
    public InputStream getBodyStream() {
        return bodyStream;
    }
    public void setInputStream(InputStream inputStream){
        this.bodyStream = inputStream;
    }

    public void addHeader(String key, String value){
        this.headers.putIfAbsent(key, value);
    }

    public String getHeader(String key){
        return this.headers.getOrDefault(key, "");
    }
}
