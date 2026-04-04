package src.main.server.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A bounded InputStream that prevents reading past the specified Content-Length.
 * This is critical for HTTP/1.1 Keep-Alive connections.
 */
public class HTTPInputStream extends InputStream {
    private final InputStream delegate;
    private long remaining;

    public HTTPInputStream(InputStream delegate, long contentLength) {
        // If content length is negative, default to 0 to prevent infinite reads
        this.remaining = Math.max(0, contentLength);
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) {
            return -1; // End of body boundary
        }

        int b = delegate.read();
        if (b != -1) {
            remaining--;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) {
            return -1;
        }

        // Ensure we don't ask for more bytes than are left in the body
        int toRead = (int) Math.min(len, remaining);
        int bytesRead = delegate.read(b, off, toRead);

        if (bytesRead != -1) {
            remaining -= bytesRead;
        }
        return bytesRead;
    }

    @Override
    public int available() throws IOException {
        // The available bytes should not exceed our remaining body boundary
        return (int) Math.min(delegate.available(), remaining);
    }

    /**
     * Pulls and discards the remaining bytes in the body.
     * * @param maxDrainBytes The security limit to prevent draining massive payloads.
     * @return The number of bytes drained.
     * @throws IOException If the remaining bytes exceed the max drain limit.
     */
    public int drain(long maxDrainBytes) throws IOException {
        if (remaining <= 0) {
            return 0; // Already clean
        }
        if (remaining > maxDrainBytes) {
            throw new IOException("Too many bytes left to drain: " + remaining + ". Socket must be closed.");
        }

        int totalDrained = 0;
        byte[] garbage = new byte[8192];

        while (remaining > 0) {
            int toRead = (int) Math.min(remaining, garbage.length);
            int bytesRead = delegate.read(garbage, 0, toRead);

            if (bytesRead == -1) break; // Socket closed early

            remaining -= bytesRead;
            totalDrained += bytesRead;
        }

        return totalDrained;
    }
}