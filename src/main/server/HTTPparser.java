package src.main.server;

import java.io.IOException;
import java.io.InputStream;

public interface HTTPparser {
    HTTPRequest parse(InputStream inputStream) throws IOException;
}
