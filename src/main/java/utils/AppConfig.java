package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration from config.properties.
 * Values can be overridden by system properties (-Dfitsense.web.url=...).
 */
public final class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {}
    }

    private AppConfig() {}

    public static String get(String key, String defaultValue) {
        // System property takes priority
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys;
        return props.getProperty(key, defaultValue);
    }
}
