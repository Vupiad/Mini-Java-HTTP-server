package src.main.server.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import src.main.server.HTTPResponse; // Assuming your response is here

public class HTTPOutputStream extends OutputStream {
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
     */
    private void commit() throws IOException {
        if (committed) return;

        // 1. Write Status Line (e.g., HTTP/1.1 200 OK)
        String statusLine = "HTTP/1.1 " + response.getStatus() + " " + response.getStatusMessage() + "\r\n";
        rawSocketOut.write(statusLine.getBytes(StandardCharsets.UTF_8));

        // 2. Write Headers
        for (Map.Entry<String, List<String>> entry : response.getHeadersMap().entrySet()) {
            for (String value : entry.getValue()) {
                String header = entry.getKey() + ": " + value + "\r\n";
                rawSocketOut.write(header.getBytes(StandardCharsets.UTF_8));
            }
        }

        // 3. Write the empty line separator
        rawSocketOut.write("\r\n".getBytes(StandardCharsets.UTF_8));

        committed = true;
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
        rawSocketOut.close();
    }

}