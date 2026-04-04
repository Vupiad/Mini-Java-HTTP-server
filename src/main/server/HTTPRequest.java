package src.main.server;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

import src.main.server.utils.CaseInsensitiveMap;

public class HTTPRequest {
    public enum Method { GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE }

    private Method method;

    private String uri;

    private String version;

    private String ipAddress;

    private String scheme;

    private int port;

    private  InputStream bodyStream;


    private final Map<String, List<String>> headers = new LinkedHashMap<>();

    public HTTPRequest(String scheme, int port, String ipAddress){
        this.scheme = scheme;
        this.port = port;
        this.ipAddress = ipAddress;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getURI() {
        return this.uri;
    }

    public void setPath(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setBodyStream(InputStream bodyStream) {
        this.bodyStream = bodyStream;
    }

    // The Handler calls this only if it needs the data
    public InputStream getBodyStream() {
        return bodyStream;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void addHeader(String name, String value) {
        this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }
    public List<String> getHeaders(String key) {
        // 1. Guard against null keys to prevent NullPointerException
        if (key == null || key.isBlank()) {
            return List.of();
        }
        return headers.getOrDefault(key.toLowerCase(Locale.ROOT), List.of());
    }

    public String getHeader(String name) {
        List<String> values = getHeaders(name);

        return values.isEmpty() ? null : values.get(0);
    }

    public String getHeader(String name, String defaultValue) {
        String value = getHeader(name);
        return value != null ? value : defaultValue;
    }


    public boolean hasBody() {
        String cl = getHeaders("content-length").get(0);
        String te = getHeaders("transfer-encoding").get(0);
        return (cl != null && Long.parseLong(cl) > 0) || te != null;
    }

    public String getIpAddress(){
        return this.ipAddress;
    }
    public void setIpAddress(String ipAddress){
        this.ipAddress = ipAddress;
    }

}
