package src.main.test.java.server;


import org.junit.jupiter.api.Test;
import src.main.java.server.HTTPRequest;
import src.main.java.server.HTTPParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HTTPParserTest {

    @Test
    void testParseBasicGetRequest() throws IOException {
        // 1. Simulate the raw string coming from a browser (Notice the \r\n)
        String rawRequest = "GET /index.html HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "User-Agent: curl/7.81.0\r\n" +
                "Accept: */*\r\n" +
                "\r\n"; // End of headers boundary

        // 2. Wrap it in the streams exactly like the Worker does
        ByteArrayInputStream bais = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
        PushbackInputStream pbis = new PushbackInputStream(bais, 8192);
        byte[] buffer = new byte[8192];

        // 3. Create the empty request object
        HTTPRequest request = new HTTPRequest("http", 8080, "127.0.0.1");

        // 4. Run the parser!
        HTTPParser.parse(pbis, buffer, request);

        // 5. Verify the results
        assertEquals("GET", request.getMethod().toString());
        assertEquals("/index.html", request.getURI());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("localhost:8080", request.getHeader("Host"));
        assertEquals("curl/7.81.0", request.getHeader("User-Agent"));
    }


}