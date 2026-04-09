package src.main.java.server.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import src.main.java.server.HTTPResponse; // Assuming your response is here

public class HTTPOutputStream extends OutputStream {
    // Priority 1: Cache commonly used byte arrays (UTF-8 encoded string literals)
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] COLON_SPACE = ": ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HTTP_PREFIX = "HTTP/1.1 ".getBytes(StandardCharsets.UTF_8);

    // Priority 2: Cache status code message encodings
    private static final Map<Integer, byte[]> STATUS_BYTES = new HashMap<>();
    static {
        // Initialize common HTTP status codes - covers ~90% of responses
        STATUS_BYTES.put(200, "200 OK".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(201, "201 Created".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(204, "204 No Content".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(301, "301 Moved Permanently".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(302, "302 Found".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(304, "304 Not Modified".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(400, "400 Bad Request".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(401, "401 Unauthorized".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(403, "403 Forbidden".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(404, "404 Not Found".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(500, "500 Internal Server Error".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(502, "502 Bad Gateway".getBytes(StandardCharsets.UTF_8));
        STATUS_BYTES.put(503, "503 Service Unavailable".getBytes(StandardCharsets.UTF_8));
    }

    private final OutputStream rawSocketOut;
    private final HTTPResponse response;
    private boolean committed = false;

    public HTTPOutputStream(OutputStream rawSocketOut, HTTPResponse response) {
        this.rawSocketOut = rawSocketOut;
        this.response = response;
    }

    /**
     * The "Secret Sauce": Before writing any body bytes,
     * we must send the Status Line and Headers.
     * Optimized with Priority 1 & 2: Cached byte arrays and status codes.
     */
    private void commit() throws IOException {
        if (committed) return;

        // 1. Write Status Line (e.g., HTTP/1.1 200 OK\r\n)
        writeStatusLine();

        // 2. Write Headers using cached byte arrays for separators
        for (Map.Entry<String, List<String>> entry : response.getHeadersMap().entrySet()) {
            String keyWithColon = entry.getKey() + ": ";
            byte[] keyBytes = keyWithColon.getBytes(StandardCharsets.UTF_8);
            for (String value : entry.getValue()) {
                rawSocketOut.write(keyBytes);
                rawSocketOut.write(value.getBytes(StandardCharsets.UTF_8));
                rawSocketOut.write(CRLF);  // Use cached CRLF instead of .getBytes()
            }
        }

        // 3. Write the empty line separator (cached)
        rawSocketOut.write(CRLF);

        committed = true;
    }

    /**
     * Writes the status line using cached status bytes for common codes (Priority 2)
     */
    private void writeStatusLine() throws IOException {
        int statusCode = response.getStatus();
        byte[] cachedStatus = STATUS_BYTES.get(statusCode);

        if (cachedStatus != null) {
            // Fast path: Use pre-encoded status code + message (covers ~90% of responses)
            rawSocketOut.write(HTTP_PREFIX);      // "HTTP/1.1 "
            rawSocketOut.write(cachedStatus);     // "200 OK" (pre-encoded)
            rawSocketOut.write(CRLF);             // "\r\n"
        } else {
            // Fallback path: Build status line dynamically for non-standard codes
            String statusLine = "HTTP/1.1 " + statusCode + " " + response.getStatusMessage() + "\r\n";
            rawSocketOut.write(statusLine.getBytes(StandardCharsets.UTF_8));
        }
    }

    public boolean isCommitted(){
        return committed;
    }

    @Override
    public void write(int b) throws IOException {
        commit(); // Ensure headers are sent
        rawSocketOut.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        commit(); // Ensure headers are sent
        rawSocketOut.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (committed) {
            rawSocketOut.flush();
        }
    }

    @Override
    public void close() throws IOException {
        commit(); // If they closed without writing a body (e.g., a 204 No Content)
        rawSocketOut.flush();
    }

}