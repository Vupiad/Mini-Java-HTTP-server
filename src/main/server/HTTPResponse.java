package src.main.server;

import src.main.server.io.HTTPOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HTTPResponse {
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private int status = 200;
    private String statusMessage = "OK";
    private HTTPOutputStream outputStream;
    private Writer writer;

    /**
     * Set the HTTP Status code.
     */
    public void setStatus(int status, String message) {
        checkCommitted();
        this.status = status;
        this.statusMessage = message;
    }

    /**
     * Set a header, replacing any existing value for this name.
     */
    public void setHeader(String name, String value) {
        checkCommitted();
        if (name == null || value == null) return;

        List<String> values = new ArrayList<>();
        values.add(value);
        headers.put(name.toLowerCase(Locale.ROOT), values);
    }

    /**
     * Add a header, allowing multiple values for the same name (e.g., Set-Cookie).
     */
    public void addHeader(String name, String value) {
        checkCommitted();
        if (name == null || value == null) return;

        headers.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new ArrayList<>())
                .add(value);
    }

    public void setContentType(String type) {
        setHeader("Content-Type", type);
    }

    public void setContentLength(long length) {
        setHeader("Content-Length", String.valueOf(length));
    }

    /**
     * Returns the raw output stream. The first time the user writes to this,
     * the headers are automatically sent (Committed).
     */
    public HTTPOutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(HTTPOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Convenience method for writing text instead of raw bytes.
     */
    public Writer getWriter() {
        if (writer == null) {
            writer = new OutputStreamWriter(getOutputStream(), StandardCharsets.UTF_8);
        }
        return writer;
    }

    public boolean isCommitted() {
        return outputStream != null && outputStream.isCommitted();
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

    public Map<String, List<String>> getHeadersMap(){
        return this.headers;
    }

    public int getStatus() { return status; }
    public String getStatusMessage() { return statusMessage; }

    /**
     * Hard reset: Clears headers and status.
     * Only works if we haven't started sending data yet!
     */
    public void reset() {
        checkCommitted();
        headers.clear();
        status = 200;
        statusMessage = "OK";
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        } else if (outputStream != null) {
            outputStream.close();
        }
    }

    private void checkCommitted() {
        if (isCommitted()) {
            throw new IllegalStateException("The response has already been committed. You cannot modify headers or status now.");
        }
    }
}