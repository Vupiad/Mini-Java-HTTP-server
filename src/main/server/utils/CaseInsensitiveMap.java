package src.main.server.utils;
import java.util.HashMap;
import java.util.Map;

public class CaseInsensitiveMap extends HashMap<String, String>{
    @Override
    public String put(String key, String value) {
        return super.put(key.toLowerCase(), value);
    }

    @Override
    public String get(Object key) {
        return super.get(key.toString().toLowerCase());
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key.toString().toLowerCase());
    }

    @Override
    public String remove(Object key) {
        return super.remove(key.toString().toLowerCase());
    }
}
