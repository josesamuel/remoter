package remoter;

import java.util.HashMap;
import java.util.Map;


/**
 * Retrieve any global properties send along with any remote call.
 * Properties can be send using {@link RemoterProxy#setRemoterGlobalProperties(Map)}
 */
public class RemoterGlobalProperties {

    private static ThreadLocal<Map<String, Object>> globalProperties = new ThreadLocal<>();

    /**
     * Get value of the given global property if any. Returns null otherwise.
     */
    public static Object get(String key) {
        Map<String, Object> prop = globalProperties.get();
        if (prop != null) {
            return prop.get(key);
        }
        return null;
    }


    /**
     * Internally used
     */
    public static void set(Map properties) {
        reset();
        if (properties != null) {
            Map<String, Object> prop = new HashMap<>();
            for (Object key : properties.keySet()) {
                prop.put(key.toString(), properties.get(key));
            }
            globalProperties.set(prop);
        }
    }

    /**
     * Internally used to reset
     */
    public static void reset() {
        globalProperties.remove();
    }
}
