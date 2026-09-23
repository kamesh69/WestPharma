package com.westpharma.aisdet.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads config.properties and overlays System properties / env-style -D overrides.
 */
public final class ConfigReader {

    private static final Properties PROPS = new Properties();
    private static Path projectRoot;

    static {
        reload();
    }

    private ConfigReader() {
    }

    public static synchronized void reload() {
        PROPS.clear();
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load config.properties", e);
        }
        projectRoot = resolveProjectRoot();
    }

    private static Path resolveProjectRoot() {
        String override = System.getProperty("project.root");
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path probe = cwd;
        for (int i = 0; i < 6; i++) {
            if (Files.exists(probe.resolve("pom.xml")) && Files.exists(probe.resolve("commons"))) {
                return probe;
            }
            probe = probe.getParent();
            if (probe == null) {
                break;
            }
        }
        return cwd;
    }

    public static Path getProjectRoot() {
        return projectRoot;
    }

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        return PROPS.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public static Path resolveFromRoot(String relativeOrAbsolute) {
        Objects.requireNonNull(relativeOrAbsolute, "path");
        Path p = Paths.get(relativeOrAbsolute);
        if (p.isAbsolute()) {
            return p;
        }
        return getProjectRoot().resolve(p).normalize();
    }
}
