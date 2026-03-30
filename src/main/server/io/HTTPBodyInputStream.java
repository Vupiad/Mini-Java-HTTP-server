package src.main.server.io;
import java.io.IOException;
import java.io.InputStream;

public class HTTPBodyInputStream extends InputStream {
    private final InputStream delegate;
    private long remaining;

    public HTTPBodyInputStream(InputStream delegate, long contentLength) {
        this.delegate = delegate;
        this.remaining = contentLength;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) return -1; // End of body
        int b = delegate.read();
        if (b != -1) remaining--;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException{
        if (remaining <= 0) return -1;
        int toRead = (int) Math.min(len, remaining);
        int read = delegate.read(b, off, toRead);
        if (read != -1) remaining -= read;
        return read;
    }
}
