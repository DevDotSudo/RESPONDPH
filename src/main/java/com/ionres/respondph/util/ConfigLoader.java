package com.ionres.respondph.util;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final Properties props = new Properties();

    static {
        try {
            InputStream is = ConfigLoader.class
                    .getClassLoader()
                    .getResourceAsStream("config/Outlet.properties");

            if (is == null) {
                throw new RuntimeException("config/Outlet.properties not found in classpath");
            }

            props.load(is);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load Outlet.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
