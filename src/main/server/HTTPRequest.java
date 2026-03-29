package src.main.server;

import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
    private String method;
    private String path;
    private String version;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body;

    // Getters and Setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public void addHeader(String key, String value) {
        headers.put(key.trim(), value.trim());
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public boolean hasBody() {
        return headers.containsKey("Content-Length") ||
                "chunked".equalsIgnoreCase(headers.get("Transfer-Encoding"));
    }
}
