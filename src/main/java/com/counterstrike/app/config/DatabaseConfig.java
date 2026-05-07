package com.counterstrike.app.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public record DatabaseConfig(String url, String user, String password) {
    private static final String DEFAULT_CONFIG_FILE = "app.properties";

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        Path source = findConfigPath();

        if (source != null) {
            try (InputStream inputStream = Files.newInputStream(source)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read database config: " + source, exception);
            }
        } else {
            try (InputStream inputStream = DatabaseConfig.class.getResourceAsStream("/app.properties")) {
                if (inputStream != null) {
                    properties.load(inputStream);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read classpath app.properties.", exception);
            }
        }

        if (properties.isEmpty()) {
            throw new IllegalStateException(
                    "Missing database configuration. Create app.properties in the project root from "
                            + "src/main/resources/app.properties.example, or pass -Dcs2.config=C:\\path\\to\\app.properties."
            );
        }

        return new DatabaseConfig(
                require(properties, "db.url"),
                require(properties, "db.user"),
                require(properties, "db.password")
        );
    }

    private static Path findConfigPath() {
        String explicitPath = System.getProperty("cs2.config");
        if (isPresent(explicitPath)) {
            Path path = Path.of(explicitPath.trim());
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Configured database file does not exist: " + path);
            }
            return path;
        }

        String environmentPath = System.getenv("CS2_DB_CONFIG");
        if (isPresent(environmentPath)) {
            Path path = Path.of(environmentPath.trim());
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Configured database file does not exist: " + path);
            }
            return path;
        }

        for (Path candidate : defaultConfigCandidates()) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static List<Path> defaultConfigCandidates() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of(DEFAULT_CONFIG_FILE));

        Path codeLocation = codeLocation();
        if (codeLocation != null) {
            Path baseDirectory = Files.isDirectory(codeLocation) ? codeLocation : codeLocation.getParent();
            if (baseDirectory != null) {
                candidates.add(baseDirectory.resolve(DEFAULT_CONFIG_FILE));

                Path parent = baseDirectory.getParent();
                if (parent != null) {
                    candidates.add(parent.resolve(DEFAULT_CONFIG_FILE));
                }
            }
        }

        return candidates;
    }

    private static Path codeLocation() {
        try {
            return Path.of(DatabaseConfig.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (NullPointerException | URISyntaxException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (!isPresent(value)) {
            throw new IllegalStateException("Missing required database property: " + key);
        }
        return value.trim();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
