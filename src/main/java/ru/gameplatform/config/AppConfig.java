package ru.gameplatform.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties PROPERTIES = load();

    private AppConfig() {
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(PROPERTIES.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream is = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is == null) {
                throw new IllegalStateException("Файл application.properties не найден");
            }
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось прочитать application.properties", e);
        }
        return props;
    }
}