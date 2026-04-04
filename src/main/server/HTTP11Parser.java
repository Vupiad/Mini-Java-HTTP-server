package src.main.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;

public class HTTP11Parser {

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
        // 1. Parse the Request Line
        int lineEnd = findLineEnd(buffer, 0, length);
        String requestLine = new String(buffer, 0, lineEnd, StandardCharsets.UTF_8);

        String[] parts = requestLine.split(" ");
        if (parts.length >= 3) {
            // Safely convert string to Enum
            try {
                request.setMethod(HTTPRequest.Method.valueOf(parts[0].toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IOException("Unsupported or malformed HTTP method: " + parts[0]);
            }

            request.setPath(parts[1]);
            request.setVersion(parts[2]);
        } else {
            throw new IOException("Malformed request line: " + requestLine);
        }

        // 2. Parse the Headers
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

    private static void parseSingleHeader(HTTPRequest request, byte[] buffer, int start, int end) {
        int colonPos = -1;
        for (int i = start; i < end; i++) {
            if (buffer[i] == ':') {
                colonPos = i;
                break;
            }
        }

        // If there's no colon, it's a malformed header, so we skip it
        if (colonPos != -1) {
            String key = new String(buffer, start, colonPos - start, StandardCharsets.UTF_8).trim();
            String value = new String(buffer, colonPos + 1, end - (colonPos + 1), StandardCharsets.UTF_8).trim();
            request.addHeader(key, value);
        }
    }
}