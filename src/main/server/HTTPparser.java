package src.main.server;

import java.io.IOException;
import java.io.InputStream;

public interface HTTPparser {
    public HTTPRequest parse(InputStream inputStream) throws IOException;
}
