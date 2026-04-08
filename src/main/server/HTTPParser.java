package src.main.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HTTPParser {
    // Approach 3: Common header names - interned for reuse across requests
    private static final Map<String, String> COMMON_HEADERS = new HashMap<>();
    static {
        String[] headers = {
            "content-length", "connection", "content-type", "host",
            "user-agent", "accept", "cache-control", "transfer-encoding",
            "accept-encoding", "accept-language", "keep-alive", "authorization"
        };
        for (String h : headers) {
            COMMON_HEADERS.put(h, h.intern());
        }
    }

    /**
     * Parses the HTTP request preamble (request line and headers).
     * This parser is "pure" - it only reads bytes and populates the request object.
     */
    public static void parse(PushbackInputStream pushbackIn, byte[] buffer, HTTPRequest request) throws IOException {
        // 1. Read raw bytes into the buffer until we hit the \r\n\r\n boundary
        int totalRead = readPreamble(pushbackIn, buffer);

        // 2. Locate the exact index where headers end
        int separatorIndex = findHeaderEnd(buffer, totalRead);
        if (separatorIndex == -1) {
            throw new IOException("Header block is too large for the provided buffer or malformed.");
        }

        // 3. If we read past the headers into the body, push those bytes back
        handleBodyPushback(pushbackIn, buffer, totalRead, separatorIndex);

        // 4. Extract text from the buffer and populate the HTTPRequest
        populateRequest(request, buffer, separatorIndex);
    }

    private static int readPreamble(InputStream in, byte[] buffer) throws IOException {
        int totalRead = 0;
        int max = buffer.length;

        while (totalRead < max) {
            int read = in.read(buffer, totalRead, max - totalRead);
            if (read == -1) {
                if (totalRead == 0) {
                    throw new IOException("Client closed connection before sending data.");
                }
                break; // Client sent some data then closed, process what we have
            }

            totalRead += read;

            // Stop reading from the socket as soon as we have the full header block
            if (findHeaderEnd(buffer, totalRead) != -1) {
                return totalRead;
            }
        }

        if (totalRead == max && findHeaderEnd(buffer, totalRead) == -1) {
            throw new IOException("Headers exceeded maximum buffer size.");
        }

        return totalRead;
    }

    private static void handleBodyPushback(PushbackInputStream in, byte[] buffer, int totalRead, int separatorIndex) throws IOException {
        // The separator is \r\n\r\n, which is 4 bytes long.
        // Anything after separatorIndex + 4 belongs to the body.
        int bodyBytesInScanner = totalRead - (separatorIndex + 4);
        if (bodyBytesInScanner > 0) {
            in.unread(buffer, separatorIndex + 4, bodyBytesInScanner);
        }
    }

    private static void populateRequest(HTTPRequest request, byte[] buffer, int length) throws IOException {
        // 1. Parse the Request Line using Approach 1 (byte-level parsing)
        int lineEnd = findLineEnd(buffer, 0, length);
        parseRequestLine(request, buffer, 0, lineEnd);

        // 2. Parse the Headers with Approach 2 (smart trimming) and Approach 3 (interning)
        int currentPos = lineEnd + 2;
        while (currentPos < length) {
            int nextLineEnd = findLineEnd(buffer, currentPos, length);

            if (nextLineEnd == currentPos) {
                break;
            }

            parseSingleHeader(request, buffer, currentPos, nextLineEnd);
            currentPos = nextLineEnd + 2;
        }
    }

    /**
     * Approach 1: Parse request line directly from bytes without split() or string manipulation
     */
    private static void parseRequestLine(HTTPRequest request, byte[] buffer, int start, int end) throws IOException {
        // Find first space (method end)
        int firstSpace = -1;
        for (int i = start; i < end; i++) {
            if (buffer[i] == ' ') {
                firstSpace = i;
                break;
            }
        }

        if (firstSpace == -1) {
            throw new IOException("Malformed request line: missing space after method");
        }

        // Parse method from bytes (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE)
        HTTPRequest.Method method = parseMethodFromBytes(buffer, start, firstSpace);
        request.setMethod(method);

        // Find second space (path end)
        int secondSpace = -1;
        for (int i = firstSpace + 1; i < end; i++) {
            if (buffer[i] == ' ') {
                secondSpace = i;
                break;
            }
        }

        if (secondSpace == -1) {
            throw new IOException("Malformed request line: missing space after path");
        }

        // Extract path directly (from firstSpace+1 to secondSpace)
        String path = new String(buffer, firstSpace + 1, secondSpace - firstSpace - 1, StandardCharsets.UTF_8);
        request.setPath(path);

        // Extract version directly (from secondSpace+1 to end)
        String version = new String(buffer, secondSpace + 1, end - secondSpace - 1, StandardCharsets.UTF_8);
        request.setVersion(version);
    }

    /**
     * Parse HTTP method directly from bytes without String creation for common cases
     */
    private static HTTPRequest.Method parseMethodFromBytes(byte[] buffer, int start, int end) throws IOException {
        int len = end - start;

        // Optimize for common HTTP methods by byte length and pattern matching
        if (len == 3) {
            if (matchBytes(buffer, start, "GET")) return HTTPRequest.Method.GET;
            if (matchBytes(buffer, start, "PUT")) return HTTPRequest.Method.PUT;
        } else if (len == 4) {
            if (matchBytes(buffer, start, "POST")) return HTTPRequest.Method.POST;
            if (matchBytes(buffer, start, "HEAD")) return HTTPRequest.Method.HEAD;
        } else if (len == 5) {
            if (matchBytes(buffer, start, "PATCH")) return HTTPRequest.Method.PATCH;
            if (matchBytes(buffer, start, "TRACE")) return HTTPRequest.Method.TRACE;
        } else if (len == 6) {
            if (matchBytes(buffer, start, "DELETE")) return HTTPRequest.Method.DELETE;
        } else if (len == 7) {
            if (matchBytes(buffer, start, "OPTIONS")) return HTTPRequest.Method.OPTIONS;
        }

        // Fallback: convert to String and use enum valueOf for non-standard methods
        String methodStr = new String(buffer, start, len, StandardCharsets.UTF_8);
        try {
            return HTTPRequest.Method.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IOException("Unsupported or malformed HTTP method: " + methodStr);
        }
    }

    /**
     * Case-insensitive byte pattern matching for HTTP methods
     */
    private static boolean matchBytes(byte[] buffer, int pos, String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            byte b = buffer[pos + i];
            char c = pattern.charAt(i);
            // Case-insensitive ASCII comparison (uppercase both sides)
            if (((b >= 'a' && b <= 'z') ? (b - 32) : b) != 
                ((c >= 'a' && c <= 'z') ? (c - 32) : c)) {
                return false;
            }
        }
        return true;
    }

    private static int findHeaderEnd(byte[] buf, int length) {
        for (int i = 0; i < length - 3; i++) {
            if (buf[i] == '\r' && buf[i+1] == '\n' && buf[i+2] == '\r' && buf[i+3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private static int findLineEnd(byte[] buffer, int start, int end) {
        for (int i = start; i < end - 1; i++) {
            if (buffer[i] == '\r' && buffer[i+1] == '\n') {
                return i;
            }
        }
        return end;
    }

    private static void parseSingleHeader(HTTPRequest request, byte[] buffer, int start, int end) throws IOException {
        // Find colon separator
        int colonPos = -1;
        for (int i = start; i < end; i++) {
            if (buffer[i] == ':') {
                colonPos = i;
                break;
            }
        }

        if (colonPos == -1) {
            // Malformed header (no colon), skip it
            return;
        }

        // Approach 2: Smart trimming for key - find first/last non-whitespace byte
        int keyStart = start;
        int keyEnd = colonPos - 1;
        
        // Skip leading whitespace
        while (keyStart <= keyEnd && buffer[keyStart] <= 32) keyStart++;
        // Skip trailing whitespace
        while (keyEnd >= keyStart && buffer[keyEnd] <= 32) keyEnd--;

        if (keyStart > keyEnd) {
            // Empty key, skip
            return;
        }

        // Create key string with already-trimmed bounds (only once, no trim() call)
        String key = new String(buffer, keyStart, keyEnd - keyStart + 1, StandardCharsets.UTF_8).toLowerCase();
        
        // Approach 3: Try to use interned common header name
        String keyInterned = COMMON_HEADERS.get(key);
        if (keyInterned == null) {
            // Not a common header, still intern for potential reuse
            keyInterned = key.intern();
        }

        // Approach 2: Smart trimming for value - find first/last non-whitespace byte
        int valueStart = colonPos + 1;
        int valueEnd = end - 1;
        
        // Skip leading whitespace
        while (valueStart <= valueEnd && buffer[valueStart] <= 32) valueStart++;
        // Skip trailing whitespace
        while (valueEnd >= valueStart && buffer[valueEnd] <= 32) valueEnd--;

        // Create value string with already-trimmed bounds (only once, no trim() call)
        String value;
        if (valueStart <= valueEnd) {
            value = new String(buffer, valueStart, valueEnd - valueStart + 1, StandardCharsets.UTF_8);
        } else {
            value = "";
        }

        request.addHeader(keyInterned, value);
    }
}