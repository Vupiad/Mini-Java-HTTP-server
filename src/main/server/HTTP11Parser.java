package src.main.server;

import src.main.server.io.HTTPBodyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class HTTP11Parser implements HTTPparser{
    private static final int MAX_HEADER_SIZE = 8192;
    public HTTPRequest parse(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackIn = new PushbackInputStream(inputStream, MAX_HEADER_SIZE);

        byte[] buffer = new byte[MAX_HEADER_SIZE];
        int totalRead = 0;
        int separatorIndex = -1;

        while(totalRead < MAX_HEADER_SIZE){
            int read = pushbackIn.read(buffer, totalRead, MAX_HEADER_SIZE - totalRead);
            if(read == -1) return null;

            totalRead += read;

            separatorIndex = findHeaderEnd(buffer, totalRead);
            if(separatorIndex != -1){
                break;
            }
        }
        if(separatorIndex == -1){
            throw new IOException("header block is too large");
        }

        //push back data if we over read it
        int bodyBytesInScanner = totalRead - (separatorIndex + 4);

        if(bodyBytesInScanner > 0){
            pushbackIn.unread(buffer, separatorIndex + 4, bodyBytesInScanner);
        }

        HTTPRequest request = parseHeaderHeaders(buffer, 0, separatorIndex);

        InputStream bodyStream = pushbackIn;
        String contentLength = request.getHeader("content-length");
        if(!Objects.equals(contentLength, "")){
            bodyStream = new HTTPBodyInputStream(bodyStream, Long.parseLong(contentLength));
        }

        request.setInputStream(bodyStream);
        return request;
    }


    private HTTPRequest parseHeaderHeaders(byte[] buffer, int offset, int length){
        int lineEnd = findLineEnd(buffer, offset, length);
        String requestLine = new String(buffer, offset, lineEnd - offset, StandardCharsets.UTF_8);

        String[] parts = requestLine.split(" ");
        HTTPRequest request = new HTTPRequest(parts[0], parts[1], parts[2]);

        // 2. Parse the rest of the headers
        int currentPos = lineEnd + 2; // Skip the \r\n
        while (currentPos < length) {
            int nextLineEnd = findLineEnd(buffer, currentPos, length);
            if (nextLineEnd == currentPos) break; // Found the empty line

            // Extract key and value directly from the byte buffer
            parseSingleHeader(request, buffer, currentPos, nextLineEnd);

            currentPos = nextLineEnd + 2; // Move to next line
        }

        return request;
    }

    private int findHeaderEnd(byte[] buf, int length) {
        for (int i = 0; i < length - 3; i++) {
            if (buf[i] == '\r' && buf[i+1] == '\n' &&
                    buf[i+2] == '\r' && buf[i+3] == '\n') {
                return i;
            }
        }
        return -1;
    }
    private int findLineEnd(byte[] buffer, int start, int end) {
        for (int i = start; i < end - 1; i++) {
            if (buffer[i] == '\r' && buffer[i+1] == '\n') {
                return i;
            }
        }
        return end;
    }
    private void parseSingleHeader(HTTPRequest request, byte[] buffer, int start, int end) {
        // 1. Find the colon separator within this specific line
        int colonPos = -1;
        for (int i = start; i < end; i++) {
            if (buffer[i] == ':') {
                colonPos = i;
                break;
            }
        }

        if (colonPos == -1) {
            return; // Malformed header line, skip it
        }

        // 2. Extract Key (trimmed)
        // We convert only this small slice to a String
        String key = new String(buffer, start, colonPos - start, StandardCharsets.UTF_8).trim();

        // 3. Extract Value (trimmed)
        // The value starts after the colon
        int valueStart = colonPos + 1;
        String value = new String(buffer, valueStart, end - valueStart, StandardCharsets.UTF_8).trim();

        // 4. Add to the Case-Insensitive Map in your Request object
        request.addHeader(key, value);
    }
}
