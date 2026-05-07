package com.counterstrike.app.db;

import com.counterstrike.app.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private final DatabaseConfig config;

    public Database(DatabaseConfig config) {
        this.config = config;
    }

    public Connection getConnection() throws SQLException {
        loadOracleDriverIfPresent();
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    public void testConnection() throws SQLException {
        try (Connection ignored = getConnection()) {
            // Opening and closing the connection validates URL, credentials, and JDBC driver availability.
        }
    }

    private void loadOracleDriverIfPresent() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ignored) {
            // DriverManager will still discover JDBC 4 drivers from the runtime classpath.
        }
    }
}
